package com.smartmi.loopvideo.network.download

import android.util.Log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/**
 * @author: Chen
 * @createTime: 2022/11/1 10:05
 * @description:
 **/
private const val TAG = "Downloader"

class Downloader private constructor(private val fileManager: FileMananger) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build()
    }
    val downloadTask = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    private fun createFile(filename: String): File {
        return fileManager.createFile(filename)
    }

    suspend fun download(url: String): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url(url)
            get()
        }.build()
        val downloadInfo: DownloadInfo = DownloadInfo.Waiting(url).apply { emit() }
        suspend fun downloadStateUpdate(url: String, index: Int, progress: Float) {
            val info = downloadTask.value.get(url)
            if (info is DownloadInfo.Running) {
                info.update(index, progress).emit()
            }
        }
        try {
            val call = client.newCall(request)
            val response = call.execute()
            val contentSize = response.body?.contentLength()
            val file = createFile(response.headers["filename"] ?: url.split("/").last())
            val ranges = response.headers["accept-ranges"]

            val threadCount = if (contentSize == null || (ranges == null || ranges != "bytes")) {
                1
            } else {
                MAX_THREAD_COUNT
            }
            log(response.headers)
            downloadInfo.start(List(threadCount) { 0f }, file.absolutePath).emit()
            log("download path = ${file.absolutePath}")
            val downloadList = mutableListOf<Deferred<Unit>>()
            repeat(threadCount) {
                downloadList.add(async(start = CoroutineStart.LAZY) {
                    _download(
                        url,
                        file,
                        thread = it,
                        contentSize,
                        threadCount,
                        ::downloadStateUpdate
                    )
                })
            }
            downloadList.awaitAll()
        } catch (e: Exception) {
            downloadTask.value[url]?.failed(e)?.emit()
        }

    }

    private suspend fun _download(
        url: String,
        file: File,
        thread: Int,
        contentSize: Long?,
        threadCount: Int,
        update: suspend (String, Int, Float) -> Unit
    ) {
        var start = 0L
        var end = 0L
        var size: Long? = null
        if (contentSize != null && threadCount > 1) {
            size = contentSize / threadCount
            start = thread * size
            end = if (thread == threadCount - 1) {
                contentSize - 1
            } else {
                start + size - 1
            }
        }
        val request = Request.Builder().apply {
            url(url)
            get()
            if (contentSize != null && threadCount > 1) header("Range", "bytes=$start-$end")
        }.build()
        val response = client.newCall(request).execute()
        log("download thread$thread get header = ${response.headers}")
        val body = response.body ?: throw  Exception("response body is null!")
        body.source().use { source ->
            val temp = RandomAccessFile(file, "rwd")
            temp.seek(start)
            FileOutputStream(temp.fd).sink().buffer().use { sink ->
                val buffer = sink.buffer
                var len: Long
                var saved = 0L
                val all = body.contentLength()/4096
                var count = 0L
                while (source.read(buffer, 4096L).also {
                        len = it
                        Log.d("source read", "thread-$thread: read len = $len")
                    } != -1L) {
                    sink.emit()
                    Log.d("source read", "thread-$thread: emit (count = $count, all = $all)")
                    saved += len
                    count++
                    update(
                        url,
                        thread,
                        saved / (size ?: body.contentLength()).toFloat()
                    )
                }
                Log.d("source read", "thread-$thread: read finish")
            }
            temp.close()
        }
    }

    private suspend fun DownloadInfo.emit() {
        val map = downloadTask.value
        downloadTask.emit(map.toMutableMap().apply {
            put(url, this@emit)
        })
    }

    companion object {
        private const val MAX_THREAD_COUNT = 1
        private var instance: Downloader? = null
        fun getInstance(fileManager: FileMananger): Downloader {
            return instance ?: synchronized(this) {
                instance ?: Downloader(fileManager).also {
                    instance = it
                }
            }
        }
    }

    private fun log(msg: Any) {
        Log.d(TAG, msg.toString())
    }
}

sealed class DownloadInfo(val url: String) {
    //    class Initialized(url: String) : DownloadInfo(url)
    class Waiting(url: String) : DownloadInfo(url) {
        override fun start(progress: List<Float>, path: String): Running =
            Running(url = url, progress = progress, path = path)

        override fun pause(progress: List<Float>, path: String): Pause = unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()
        override fun toString(): String {
            return "$url ==> is waiting"
        }
    }

    class Running(url: String, val path: String, val progress: List<Float>) : DownloadInfo(url) {
        override fun start(progress: List<Float>, path: String): Running = unsupportedAction()

        override fun pause(progress: List<Float>, path: String): Pause =
            Pause(url, progress = progress, path = path)

        override fun success(): Success = Success(url)

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()

        suspend fun update(index: Int, progress: Float): DownloadInfo {
            val new = this.progress.toMutableList().apply {
                set(index, progress)
            }
            var total = 0f
            new.forEach {
                total += it
            }
            total /= new.size
            return if (total >= 1f) {
                Success(url)
            } else {
                Running(url, progress = new, path = path)
            }
        }

        override fun toString(): String {
            var total = 0f
            progress.forEach {
                total += it
            }
            total /= progress.size
            return "$url is downloading,progress =  ${total * 100}%"
        }
    }

    class Pause(url: String, val path: String, val progress: List<Float>) : DownloadInfo(url) {
        override fun start(progress: List<Float>, path: String): Running =
            Running(url, path, this.progress)

        override fun pause(progress: List<Float>, path: String): Pause = unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()
        override fun toString(): String {
            return "$url is pausing"
        }
    }

    class Success(url: String) : DownloadInfo(url) {
        override fun start(progress: List<Float>, path: String): Running = unsupportedAction()

        override fun pause(progress: List<Float>, path: String): Pause = unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = unsupportedAction()

        override fun retry(): Waiting = unsupportedAction()

        override fun toString(): String {
            return "$url download success"
        }
    }

    class Failed(url: String, val error: Throwable) : DownloadInfo(url) {

        override fun start(progress: List<Float>, path: String): Running = unsupportedAction()

        override fun pause(progress: List<Float>, path: String): Pause = unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = unsupportedAction()

        override fun retry(): Waiting = Waiting(url)

        override fun toString(): String {
            return "$url download failed => $error"
        }
    }

    abstract fun start(progress: List<Float>, path: String): Running

    abstract fun pause(progress: List<Float>, path: String): Pause

    abstract fun success(): Success

    abstract fun failed(thr: Throwable): Failed

    abstract fun retry(): Waiting

    protected fun <T> unsupportedAction(): T {
        throw Exception("unsupportedAction")
    }
}