package com.smartmi.loopvideo.network.download

import android.content.Context
import com.google.common.annotations.VisibleForTesting
import java.io.File

/**
 * @author: Chen
 * @createTime: 2022/11/1 16:44
 * @description:
 **/
class FileManager(private val cacheDirectory: File) {
    companion object {
        private var _instance: FileManager? = null
        fun getInstance(context: Context): FileManager {
            return _instance ?: synchronized(this) {
                _instance ?: FileManager(context.cacheDir).also {
                    _instance = it
                }
            }
        }

        @VisibleForTesting
        fun getInstanceForTest(file: File): FileManager {
            return _instance ?: synchronized(this) {
                _instance ?: FileManager(file).also {
                    _instance = it
                }
            }
        }
    }

    fun createFile(fileName:String): File {
        return File(cacheDirectory.absolutePath+"/$fileName")
    }
}