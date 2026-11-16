package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.myapplication.DormSwapApp
    val user = app.auth.user.collectAsStateWithLifecycle().value
    if (user == null) { LoginScreen(); return }
    val ready = app.readyUser.collectAsStateWithLifecycle().value
    if (ready != user.id) {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment=androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.FEED) {

        composable(Routes.ACCOUNT) {
            AccountScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    scope.launch { app.auth.signOut() }
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
