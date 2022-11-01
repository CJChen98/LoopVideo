package com.smartmi.loopvideo

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    //https://media.githubusercontent.com/media/CJChen98/Blog/master/video/conifg.json
    private val videoView by lazy(mode = LazyThreadSafetyMode.NONE) {
        findViewById<VideoView>(R.id.videoView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.systemBars())
        videoView.apply {
            setOnPreparedListener {
                Log.d("Video", "OnPrepared:${it.duration} ")
                it.start()
                it.isLooping = true
                lifecycleScope.launch(Dispatchers.Default) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        while (true) {
                            delay(10)
                            if (!videoView.isPlaying) break
                            if (videoView.currentPosition > videoView.duration - 100) {
                                videoView.seekTo(0)
                            }
                        }
                    }
                }
            }

//            setVideoURI(Uri.parse("https://media.githubusercontent.com/media/CJChen98/Blog/master/video/video1.mov"))
//            setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.video))
        }

    }

    override fun onResume() {
        super.onResume()
        videoView.resume()
    }

    override fun onPause() {
        videoView.pause()
        super.onPause()
    }
}