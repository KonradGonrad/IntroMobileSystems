package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.core.theme.MyApplicationTheme
import com.example.myapplication.ui.addItem.Add_Item_Screen
import com.example.myapplication.ui.feed.Main_Screen
import com.example.myapplication.ui.details.Item_Detail_Screen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("feed") }

                when (currentScreen) {
                    "feed" -> Main_Screen(
                        onNavigateToAddItem = { currentScreen = "add_item" },
                        onNavigateToDetail = { currentScreen = "item_detail" }
                    )
                    "add_item" -> Add_Item_Screen(onBackClick = { currentScreen = "feed" })
                    "item_detail" -> Item_Detail_Screen(onBackClick = { currentScreen = "feed" })
                }
            }
        }
    }
}