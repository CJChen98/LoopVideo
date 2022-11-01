package com.smartmi.loopvideo.bean

/**
 * @author: Chen
 * @createTime: 2022/10/28 16:05
 * @description:
 **/
sealed class RemoteConfig {
    class Success(val data: List<String>) : RemoteConfig() {
        override fun toString(): String {
            return "{data  = $data}"
        }
    }

    class Error(val msg: String) : RemoteConfig() {
        override fun toString(): String {
            return msg
        }
    }

    object Empty : RemoteConfig()
}