package com.smartmi.loopvideo.repository

import com.smartmi.loopvideo.bean.RemoteConfig
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * @author: Chen
 * @createTime: 2022/10/28 15:39
 * @description:
 **/
@OptIn(ExperimentalSerializationApi::class)
class RemoteConfigRepository private constructor() {
    companion object {
        private val instance by lazy { RemoteConfigRepository() }
        operator fun invoke() = instance
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    fun getConfig() = flow {
        val call = okHttpClient.newCall(request = Request.Builder().apply {
            url("https://media.githubusercontent.com/media/CJChen98/Blog/master/video/conifg.json")
        }.build())
        val response = call.execute()
        val body = response.body
        if (body == null) {
            emit(RemoteConfig.Error("response body is null"))
        } else {
            try {
              body.use  {
//                  val json = it.string()
//                  val data: List<String> = Json.decodeFromString(json)
                    val data: List<String> = Json.decodeFromStream(it.byteStream())
                    emit(RemoteConfig.Success(data = data))
                }
            } catch (e: Exception) {
                emit(RemoteConfig.Error(e.toString()))
            }
        }
    }
}