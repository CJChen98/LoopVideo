package com.smartmi.loopvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.smartmi.loopvideo.compose.route.AppNavHost
import com.smartmi.loopvideo.ui.theme.LoopVideoTheme

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoopVideoTheme {
                AppNavHost()
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

    }
}