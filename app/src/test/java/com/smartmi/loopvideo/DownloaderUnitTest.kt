package com.smartmi.loopvideo

import com.smartmi.loopvideo.network.download.DownloadInfo
import com.smartmi.loopvideo.network.download.Downloader
import com.smartmi.loopvideo.network.download.FileManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

/**
 * @author: Chen
 * @createTime: 2022/11/1 11:10
 * @description:
 **/

class DownloaderUnitTest {
    private val downloader =
        Downloader.getInstance(FileManager.getInstanceForTest(File("./")))

    @Test
    fun testDownload() {
        runBlocking {
            launch {
//                downloader.download("https://media.githubusercontent.com/media/CJChen98/Blog/master/video/video2.mp4")
                downloader.download("https://media.githubusercontent.com/media/CJChen98/Blog/master/img/image-20200620181959742.png")
            }
            launch {
                downloader.downloadTask.collectLatest {
                    val task = it.values.firstOrNull() ?: return@collectLatest
                    println(task)
                    if (task is DownloadInfo.Success || task is DownloadInfo.Failed) {
                        assert(true)
                        cancel()
                    }
                }
            }
        }
    }
}