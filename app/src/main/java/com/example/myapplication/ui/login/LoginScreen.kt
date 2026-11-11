package com.example.myapplication.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.core.theme.DormGreen
import com.example.myapplication.core.theme.DormGreenLight
import com.example.myapplication.core.theme.TextMuted

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    // Keep the demo password in memory only.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val submit = {
        if (login == "admin" && password == "admin") {
            focusManager.clearFocus()
            onLoggedIn()
        } else {
            hasError = true
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .consumeWindowInsets(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = DormGreenLight) {
                Icon(Icons.Outlined.Eco, contentDescription = null, tint = DormGreen,
                    modifier = Modifier.padding(20.dp).size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("DormSwap", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Give things a second life", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(36.dp))
            Text("Welcome back", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Text("Sign in to find and share items in your dorm.", color = TextMuted,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp))
            OutlinedTextField(
                value = login,
                onValueChange = { login = it; hasError = false },
                label = { Text("Login") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                isError = hasError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; hasError = false },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                isError = hasError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (hasError) {
                Text("Incorrect login or password.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite })
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = submit, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Sign in")
            }
        }
    }
}
