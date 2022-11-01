package com.smartmi.loopvideo.compose.page.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

/**
 * @author: Chen
 * @createTime: 2022/10/28 14:41
 * @description:
 **/
const val homePageRoute = "hom_page_route"
fun NavController.navigationToHomePage(navOptions: NavOptions) {
    navigate(homePageRoute, navOptions)
}

fun NavGraphBuilder.homePage() {
    composable(homePageRoute) {
        HomePage()
    }
}