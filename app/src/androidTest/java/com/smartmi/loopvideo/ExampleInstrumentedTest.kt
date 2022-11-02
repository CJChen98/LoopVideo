package com.smartmi.loopvideo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartmi.loopvideo.network.download.DownloadInfo
import com.smartmi.loopvideo.network.download.Downloader
import com.smartmi.loopvideo.network.download.FileManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("com.smartmi.loopvideo", appContext.packageName)
        val fileManager = FileManager.getInstance(appContext)
        val downloader = Downloader.getInstance(fileManager)
        runBlocking {
            launch { downloader.download("https://media.githubusercontent.com/media/CJChen98/Blog/master/video/video2.mp4") }
            launch {
                downloader.downloadTask.collectLatest {
                    println(it.values.toString())
                    if (it.values.firstOrNull() is DownloadInfo.Success) {
                        cancel()
                    }
                }
            }
        }
    }
}