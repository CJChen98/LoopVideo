package com.smartmi.loopvideo.compose.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.smartmi.loopvideo.compose.page.home.homePage
import com.smartmi.loopvideo.compose.page.home.homePageRoute

/**
 * @author: Chen
 * @createTime: 2022/10/28 17:13
 * @description:
 **/

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = homePageRoute
) {
    NavHost(
        navController = navController,
        modifier = modifier,
        startDestination = startDestination
    ) {
        homePage()
    }
}