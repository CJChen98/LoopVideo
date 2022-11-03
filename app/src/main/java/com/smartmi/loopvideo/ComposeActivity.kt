package com.smartmi.loopvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.smartmi.loopvideo.network.download.DownloadInfo
import com.smartmi.loopvideo.network.download.Downloader
import com.smartmi.loopvideo.network.download.FileManager
import com.smartmi.loopvideo.ui.theme.LoopVideoTheme
import kotlinx.coroutines.launch

class ComposeActivity : ComponentActivity() {
    private val downloader by lazy { Downloader.getInstance(FileManager.getInstance(this)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoopVideoTheme {
//                AppNavHost()
                DownloadPage(downloader)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPage(downloader: Downloader) {
    val scope = rememberCoroutineScope()
    val tasks by downloader.downloadTask.collectAsState()
    val list = remember(tasks) {
        tasks.values.toList()
    }
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "Download") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    downloader.download(
                        "https://media.githubusercontent.com/media/CJChen98/Blog/master/video/video2.mp4",
                        4
                    )
                }
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "add")
            }
        }) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {

            items(list.size) { i ->
                DownloadInfoItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    info = list[i]
                )
            }
        }
    }
}

@Composable
fun DownloadInfoItem(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(12.dp),
    info: DownloadInfo
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
        val status: String
        var percent: String? = null
        when (info) {
            is DownloadInfo.Waiting -> {
                status = "等待中"
            }
            is DownloadInfo.Running -> {
                status = "下载中"
                percent = info.percent
            }
            is DownloadInfo.Pause -> {
                status = "已暂停"
                percent = info.percent
            }
            is DownloadInfo.Success -> {
                status = "完成,已下载至: ${info.path}"
            }
            is DownloadInfo.Failed -> {
                status = "失败: ${info.error}"
            }
        }
        Column(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(8f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = info.url,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                percent?.let {
                    Text(
                        it,
                        modifier = Modifier.weight(3f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

            }

            if (info is DownloadInfo.Running) {
                val threadSize = info.byteCount / info.threadCount
                val percents = info.process.map { (it.toFloat() / threadSize).coerceIn(0f, 1f) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (percent in percents) {
                        LinearProgressIndicator(progress = percent, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}