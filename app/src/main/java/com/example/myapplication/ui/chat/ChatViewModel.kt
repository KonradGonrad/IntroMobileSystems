package com.example.myapplication.ui.chat

import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatViewModel(saved: SavedStateHandle,private val repo: ItemRepository) : ViewModel() {
    val conversationId: String? = saved["conversationId"]
    val conversations=repo.conversations.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val messages=repo.messages(conversationId.orEmpty()).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val online=repo.isOnline
    val sync=repo.syncStatus
    var draft by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    suspend fun refresh()=repo.sync()
    fun send() {
        if(busy || conversationId==null) return
        if(draft.trim().length !in 1..2000) { error="Message must have 1–2000 characters."; return }
        val text=draft; busy=true
        viewModelScope.launch {
            try { repo.sendMessage(conversationId,text); draft=""; error=null }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error=e.message }
            finally { busy=false }
        }
    }
    companion object {
        val Factory=viewModelFactory { initializer { ChatViewModel(createSavedStateHandle(),(this[APPLICATION_KEY] as DormSwapApp).repository) } }
    }
}
