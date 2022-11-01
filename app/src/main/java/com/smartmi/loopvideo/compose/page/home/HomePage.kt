@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartmi.loopvideo.compose.page.home

import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmi.loopvideo.bean.RemoteConfig
import com.smartmi.loopvideo.network.download.Downloader
import com.smartmi.loopvideo.network.download.FileMananger
import kotlinx.coroutines.launch

/**
 * @author: Chen
 * @createTime: 2022/10/28 14:38
 * @description:
 **/
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomePage(homeViewModel: HomeViewModel = viewModel()) {
    val cxt = LocalContext.current
    val downloader = remember { Downloader.getInstance(FileMananger.getInstance(cxt)) }
    val scope = rememberCoroutineScope()
    val log by downloader.downloadTask.collectAsState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(top = 100.dp),
        topBar = {
            TopAppBar(title = { Text(text = "LoopVideo") })
        }) {
        Box(modifier = Modifier
            .padding(it)
            .fillMaxSize()) {
            Button(onClick = {
                scope.launch {
                    downloader.download("https://media.githubusercontent.com/media/CJChen98/Blog/master/video/video2.mp4")
                }
            }, modifier = Modifier.align(Alignment.Center)) {
                Text(text = "download")
            }
            Text(
                text = log.values.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)

            )
        }
//        val config by homeViewModel.config.collectAsState()
//        AnimatedContent(targetState = config, modifier = Modifier.padding(it)) { state ->
//            when (state) {
//                is RemoteConfig.Error -> {
//                    GetConfigError(error = state)
//                }
//                is RemoteConfig.Success -> {
//                    ConfigList(config = state)
//                }
//                is RemoteConfig.Empty -> {
//                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        Text(text = "播放列表为空", style = MaterialTheme.typography.bodyMedium)
//                    }
//                }
//            }
//        }
//        LaunchedEffect(key1 = config) {
//            if (config is RemoteConfig.Empty) {
//                homeViewModel.getConfig()
//            }
//        }
    }
}

@Composable
fun ConfigList(config: RemoteConfig.Success) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (config.data.isEmpty()) {
            Text(text = "播放列表为空", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
            ) {
                items(config.data) { item ->
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Text(text = item.split("/").last())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GetConfigError(error: RemoteConfig.Error) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            error.msg,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}