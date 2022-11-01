package com.smartmi.loopvideo.network.download

import android.content.Context
import java.io.File

/**
 * @author: Chen
 * @createTime: 2022/11/1 16:44
 * @description:
 **/
class FileMananger(private val cacheDirectory: File) {
    companion object {
        private var _instance: FileMananger? = null
        fun getInstance(context: Context): FileMananger {
            return _instance ?: synchronized(this) {
                _instance ?: FileMananger(context.cacheDir).also {
                    _instance = it
                }
            }
        }
    }

    fun createFile(fileName:String): File {
        return File(cacheDirectory.absolutePath+"/$fileName")
    }
}