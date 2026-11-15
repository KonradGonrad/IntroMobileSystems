package com.example.myapplication.ui.login

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.auth.AuthRepository
import com.example.myapplication.data.auth.AuthValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    fun submit(email: String, password: String, name: String?, repeat: String?) {
        if (busy) return
        error = AuthValidator.validate(email,password,name,repeat)
        if (error != null) return
        busy=true
        viewModelScope.launch {
            try { auth.signIn(email,password,name) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error=e.message ?: "Cannot sign in. Check your connection and try again." }
            finally { busy=false }
        }
    }
    companion object {
        val Factory = viewModelFactory { initializer { LoginViewModel((this[APPLICATION_KEY] as DormSwapApp).auth) } }
    }
}
