package com.example.myapplication.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main_Screen(
    onNavigateToAddItem: () -> Unit = {},
    onNavigateToDetail: () -> Unit = {}) {


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

        var selectedIndex by remember { mutableIntStateOf(0) }
        val options = listOf("All", "Clothes", "Electronics", "Books")

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color.Green,
                        activeContentColor = Color.White,
                        activeBorderColor = Color(0xFF4CAF50)
                    ),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex,
                    label = { Text(label, maxLines = 1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Give them second live", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))


        Card(
            onClick = onNavigateToDetail,
            modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Desk Lamp", fontSize = 18.sp)
                Text("Like new")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))



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