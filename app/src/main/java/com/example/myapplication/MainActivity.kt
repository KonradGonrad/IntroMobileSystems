package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.core.navigation.AppNavigation
import com.example.myapplication.core.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        val app=application as DormSwapApp
        if(app.readyUser.value!=null) app.repository.requestSync()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}
