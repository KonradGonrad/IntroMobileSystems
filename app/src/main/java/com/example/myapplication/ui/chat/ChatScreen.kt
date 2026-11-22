package com.example.myapplication.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.DormSwapApp
import com.example.myapplication.data.repository.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack:()->Unit,onOpen:(String)->Unit,vm: ChatViewModel=viewModel(factory=ChatViewModel.Factory)) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val rejected by vm.rejectedMessages.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val user=(LocalContext.current.applicationContext as DormSwapApp).auth.user.value
    val conversation=conversations.find { it.id==vm.conversationId }
    val scroll=rememberLazyListState()

    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) scroll.animateScrollToItem(messages.lastIndex) }
    Scaffold(topBar={TopAppBar(title={Text(if(vm.conversationId==null) "Messages" else conversation?.otherName ?: "Conversation")},
        navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Go back")}})}) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(horizontal=16.dp)) {
            if(!online) Text("Offline · Messages will send when connected.")
            (sync as? SyncStatus.Error)?.let { Text(it.message,color=MaterialTheme.colorScheme.error) }
            if(vm.conversationId==null) {
                if(conversations.isEmpty()) Text("No conversations yet. Open an item and tap Message owner.")
                LazyColumn { items(conversations,key={it.id}) { c ->
                    ListItem(headlineContent={Text(c.otherName)},supportingContent={Text(c.title+if(c.unavailable) " · Unavailable" else "")},
                        modifier=Modifier.clickable { onOpen(c.id) }.heightIn(min=64.dp))
                    HorizontalDivider()
                } }
            } else {
                Text(conversation?.title.orEmpty(),style=MaterialTheme.typography.titleMedium)
                if(conversation?.unavailable==true) Text("Listing unavailable · Conversation preserved")
                LazyColumn(state=scroll,modifier=Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=12.dp)) {
                    items(messages,key={it.id}) { message ->
                        Surface(color=if(message.senderId==user?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(if(message.senderId==user?.id) "You" else conversation?.otherName ?: "Student",style=MaterialTheme.typography.labelMedium)
                                Text(message.body)
                                Text(when {
                                    message.id in rejected -> "Send failed · Review in Account"
                                    message.pending -> "Waiting to send"
                                    else -> java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(message.createdAt))
                                },style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                vm.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                OutlinedTextField(value=vm.draft,onValueChange={vm.draft=it},label={Text("Message")},maxLines=4,modifier=Modifier.fillMaxWidth())
                Button(onClick=vm::send,enabled=!vm.busy && vm.draft.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(vertical=8.dp).heightIn(min=48.dp)) { Text("Send") }
            }
        }
    }
}
