package com.example.myapplication.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.DormGreenLight
import com.example.myapplication.core.theme.TextMuted

@Composable
fun AccountScreen(onBackClick: () -> Unit, onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
                Text("My account", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(color = DormGreenLight, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null,
                        tint = DormGreen, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("admin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Demo account", color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Dormitory 5 · Łódź", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Text("Sign out", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
