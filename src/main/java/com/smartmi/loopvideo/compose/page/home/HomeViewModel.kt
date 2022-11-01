package com.smartmi.loopvideo.compose.page.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmi.loopvideo.bean.RemoteConfig
import com.smartmi.loopvideo.repository.RemoteConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * @author: Chen
 * @createTime: 2022/10/28 15:42
 * @description:
 **/
class HomeViewModel : ViewModel() {
    private val remoteConfigRepository = RemoteConfigRepository()
    private val _config = MutableStateFlow<RemoteConfig>(RemoteConfig.Empty)
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()
    fun getConfig() {
        viewModelScope.launch(Dispatchers.IO){
            remoteConfigRepository.getConfig().onEach {
                _config.emit(it)
            }.collect()
        }
    }
}