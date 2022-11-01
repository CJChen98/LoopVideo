package com.smartmi.loopvideo

import com.smartmi.loopvideo.bean.RemoteConfig
import com.smartmi.loopvideo.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import org.junit.Test
import java.io.File

/**
 * @author: Chen
 * @createTime: 2022/10/28 16:18
 * @description:
 **/
class RepositoryUnitTest {
    @Test
    fun getConfigTest(){
        val repository = RemoteConfigRepository()
        runBlocking {
            repository.getConfig().collectLatest { config->
                println(config)
                assert(config is RemoteConfig.Success)
            }
        }

    }
}