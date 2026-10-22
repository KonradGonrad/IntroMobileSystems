package com.example.myapplication.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(onNavigateToAddItem: () -> Unit = {}) {

    Column(modifier = Modifier.padding(16.dp)) {

        Text("DormSwap", fontSize = 30.sp)
        Text("Dormitory 5 - Łódź")

        Spacer(modifier = Modifier.height(16.dp))

        var textState by remember { mutableStateOf("") }

        OutlinedTextField(
            value = textState,
            onValueChange = { newValue ->
                textState = newValue
            },
            label = { Text("What are you looking for?") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("All")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Clothes")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Electronics")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Books")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Give them second live", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))


        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Desk Lamp", fontSize = 18.sp)
                Text("Like new")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hoodie", fontSize = 18.sp)
                Text("Very good condition")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))


        Button(
            onClick = onNavigateToAddItem,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("+ Add things you don't need")
        }

    }
}