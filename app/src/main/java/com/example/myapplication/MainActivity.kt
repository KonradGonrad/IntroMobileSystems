package com.example.myapplication

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.core.navigation.AppNavigation
import com.example.myapplication.core.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var showActivity by androidx.compose.runtime.mutableStateOf(false)
    override fun onStart() {
        super.onStart()
        (application as DormSwapApp).foreground.value = true
    }
    override fun onStop() {
        (application as DormSwapApp).foreground.value = false
        super.onStop()
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        showActivity = intent.action == "com.example.myapplication.ACTIVITY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showActivity = intent.action == "com.example.myapplication.ACTIVITY"
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation(showActivity) { showActivity = false }
            }
        }
    }
}
