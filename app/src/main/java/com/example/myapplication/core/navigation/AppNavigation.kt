package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.account.AccountScreen
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.addItem.AddItemScreen
import com.example.myapplication.ui.details.ItemDetailScreen
import com.example.myapplication.ui.feed.FeedScreen

object Routes {
    const val LOGIN = "login"
    const val ACCOUNT = "account"
    const val FEED = "feed"
    const val ADD_ITEM = "add_item"
    const val ITEM_DETAIL = "item/{itemId}"

    fun itemDetail(id: String) = "item/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.FEED) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.FEED) {
            FeedScreen(
                onAccountClick = { navController.navigate(Routes.ACCOUNT) },
                onAddItemClick = { navController.navigate(Routes.ADD_ITEM) },
                onItemClick = { id -> navController.navigate(Routes.itemDetail(id)) }
            )
        }

        composable(Routes.ADD_ITEM) {
            AddItemScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            ItemDetailScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
