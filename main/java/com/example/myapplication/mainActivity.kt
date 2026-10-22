package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.core.theme.MyApplicationTheme
import com.example.myapplication.ui.addItem.AddItemScreen
import com.example.myapplication.ui.feed.MainScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("feed") }

                when (currentScreen) {
                    "feed" -> MainScreen(onNavigateToAddItem = { currentScreen = "add_item" })
                    "add_item" -> AddItemScreen(onBackClick = { currentScreen = "feed" })
                }
            }
        }
    }
}