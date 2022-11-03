package com.smartmi.loopvideo.network.download

import android.util.Log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okhttp3.logging.HttpLoggingInterceptor
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

class Downloader private constructor(private val fileManager: FileManager) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor {
                Log.d("OkHttp", it)
            }.apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
    }
    val downloadTask = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    private fun createFile(filename: String): File {
        return fileManager.createFile(filename)
    }

    suspend fun download(url: String,threadCount: Int = MAX_THREAD_COUNT): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url(url)
        }.build()
        val downloadInfo: DownloadInfo = DownloadInfo.Waiting(url).apply { emit() }
        val updateFlow =
            MutableSharedFlow<DownloadInfo.Running.UpdateEvent>(
                extraBufferCapacity = 10,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

        suspend fun downloadStateUpdate(url: String, index: Int, saved: Long) {
            val info = downloadTask.value[url]
            if (info is DownloadInfo.Running) {
                updateFlow.emit(DownloadInfo.Running.UpdateEvent(index, saved))
            }
        }
        launch(Dispatchers.Default) {
            updateFlow.collect { event ->
                println(event)
                val info = downloadTask.value[url]
                if (info is DownloadInfo.Running) {
                    val (index, saved) = event
                    info.update(index, saved).emit()
                }
            }
        }
        try {
            val call = client.newCall(request)
            val response = call.execute()
            val contentSize = response.body?.contentLength()
            val filename = response.headers["content-disposition"]?.split(";")
                ?.find { s ->
                    s.startsWith("filename=")
                }?.replace("filename=", "")
                ?: url.split("/").last()
            val file = createFile(filename)
            log("download path = ${file.absolutePath}")
            val ranges = response.headers["accept-ranges"]
            val tCount = if (contentSize == null || (ranges == null || ranges != "bytes")) {
                1
            } else {
                threadCount.coerceAtMost(MAX_THREAD_COUNT)
            }
            downloadInfo.start(
                path = file.absolutePath,
                threadCount = tCount,
                byteCount = response.body?.contentLength()!!
            ).emit()
            response.closeQuietly()
            val downloadList = mutableListOf<Deferred<Unit>>()
            repeat(tCount) {
                downloadList.add(async(start = CoroutineStart.LAZY) {
                    _download(
                        url,
                        file,
                        thread = it,
                        contentSize,
                        tCount,
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
        update: suspend (String, Int, Long) -> Unit
    ) {
        var start = 0L
        var end = 0L
        val size: Long?
        if (contentSize != null && threadCount > 1) {
            size = contentSize / threadCount
            start = thread * size
            end = if (thread == threadCount - 1) {
                contentSize
            } else {
                start + size - 1
            }
        }

        val request = Request.Builder().apply {
            url(url)
            get()
            if (contentSize != null && threadCount > 1) header("Range", "bytes=$start-$end")
        }.build()
        val res = client.newCall(request).execute()
        res.use { response ->
            response.body?.source()?.use { source ->
                val accessFile = RandomAccessFile(file, "rws")
                accessFile.seek(start)
                FileOutputStream(accessFile.fd).sink().buffer().use { sink ->
                    val buffer = sink.buffer
                    var len: Long
                    var saved = 0L
                    while (source.read(buffer, 4096L).also {
                            len = it
                        } != -1L) {
                        sink.flush()
                        saved += len
                        update(
                            url,
                            thread,
                            saved
                        )
                    }
                }
                accessFile.closeQuietly()
            }
        }
    }

    private suspend fun DownloadInfo.emit() {
        val map = downloadTask.value
        downloadTask.emit(map.toMutableMap().apply {
            put(url, this@emit)
        })
    }

    companion object {
        private const val MAX_THREAD_COUNT = 8
        private var instance: Downloader? = null
        fun getInstance(fileManager: FileManager): Downloader {
            return instance ?: synchronized(this) {
                instance ?: Downloader(fileManager).also {
                    instance = it
                }
            }
        }
    }

    private fun log(msg: Any) {
//        Log.d(TAG, msg.toString())
        print(msg)
    }
}

sealed class DownloadInfo(val url: String) {
    //    class Initialized(url: String) : DownloadInfo(url)
    class Waiting(url: String) : DownloadInfo(url) {
        override fun start(threadCount: Int, byteCount: Long, path: String): Running =
            Running(
                url = url,
                threadCount,
                byteCount = byteCount,
                path = path,
                saved = 0,
                process = List(threadCount) { 0L })

        override fun pause(threadCount: Int, byteCount: Long, path: String): Pause =
            unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()
        override fun toString(): String {
            return "$url ==> is waiting"
        }
    }

    class Running(
        url: String,
        val threadCount: Int,
        val path: String,
        val process: List<Long>,
        val byteCount: Long,
        val saved: Long = 0L
    ) :
        DownloadInfo(url) {
        data class UpdateEvent(val index: Int, val len: Long)

        override fun start(threadCount: Int, byteCount: Long, path: String): Running =
            unsupportedAction()

        override fun pause(threadCount: Int, byteCount: Long, path: String): Pause =
            Pause(url, threadCount, path, process, byteCount, saved = saved)

        override fun success(): Success = Success(url, path)

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()

        fun update(index: Int, len: Long): DownloadInfo {
            val newProgress = process.toMutableList().apply {
                set(index, len)
            }
            var allSaved = 0L
            newProgress.forEach { allSaved += it }
            return if (allSaved >= byteCount) {
                success()
            } else {
                Running(
                    url,
                    path = path,
                    byteCount = byteCount,
                    process = newProgress,
                    threadCount = threadCount,
                    saved = allSaved
                )
            }
        }

        val percent: String
            get() = String.format("%.2f", (saved * 100f) / byteCount) + "%"

        override fun toString(): String {
            return "$url is downloading,progress =  $percent\n" +
                    "$process = $saved / $byteCount"
        }
    }

    class Pause(
        url: String,
        val threadCount: Int,
        val path: String,
        val process: List<Long>,
        val byteCount: Long,
        val saved: Long = 0L
    ) : DownloadInfo(url) {
        override fun start(threadCount: Int, byteCount: Long, path: String): Running =
            Running(url, threadCount, path, process, byteCount, saved = saved)

        override fun pause(threadCount: Int, byteCount: Long, path: String): Pause =
            unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = Failed(url, thr)

        override fun retry(): Waiting = unsupportedAction()
        override fun toString(): String {
            return "$url is pausing"
        }
        val percent: String
            get() = String.format("%.2f", (saved * 100f) / byteCount) + "%"
    }

    class Success(url: String, val path: String) : DownloadInfo(url) {
        override fun start(threadCount: Int, byteCount: Long, path: String): Running =
            unsupportedAction()

        override fun pause(threadCount: Int, byteCount: Long, path: String): Pause =
            unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = unsupportedAction()

        override fun retry(): Waiting = unsupportedAction()

        override fun toString(): String {
            return "$url download success"
        }
    }

    class Failed(url: String, val error: Throwable) : DownloadInfo(url) {

        override fun start(threadCount: Int, byteCount: Long, path: String): Running =
            unsupportedAction()

        override fun pause(threadCount: Int, byteCount: Long, path: String): Pause =
            unsupportedAction()

        override fun success(): Success = unsupportedAction()

        override fun failed(thr: Throwable): Failed = unsupportedAction()

        override fun retry(): Waiting = Waiting(url)

        override fun toString(): String {
            return "$url download failed => $error"
        }
    }

    abstract fun start(threadCount: Int, byteCount: Long, path: String): Running

    abstract fun pause(threadCount: Int, byteCount: Long, path: String): Pause

    abstract fun success(): Success

    abstract fun failed(thr: Throwable): Failed

    abstract fun retry(): Waiting

    protected fun <T> unsupportedAction(): T {
        throw Exception("unsupportedAction")
    }
}