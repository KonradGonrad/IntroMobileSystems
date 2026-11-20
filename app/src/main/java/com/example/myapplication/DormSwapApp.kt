package com.example.myapplication

import android.app.Application
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.data.auth.*
import com.example.myapplication.data.local.*
import com.example.myapplication.data.photo.PhotoStorage
import com.example.myapplication.data.remote.*
import com.example.myapplication.data.repository.ItemRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DormSwapApp : Application() {
    val appScope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
    lateinit var auth: AuthRepository; private set
    lateinit var repository: ItemRepository; private set
    lateinit var photoStorage: PhotoStorage; private set
    private val _readyUser=MutableStateFlow<String?>(null)
    val readyUser: StateFlow<String?> = _readyUser

    override fun onCreate() {
        super.onCreate()
        val client=SupabaseClient()
        auth=AuthRepository(client,SessionStore(this))
        photoStorage=PhotoStorage(this)
        val network=NetworkMonitor(this)
        appScope.launch {
            auth.user.map { it?.id }.distinctUntilChanged().collectLatest { id ->
                _readyUser.value=null
                if(id==null) return@collectLatest
                val user=auth.user.value ?: return@collectLatest
                val database=UserDatabase.create(this@DormSwapApp,id)
                val sessionJob=SupervisorJob()
                val scope=CoroutineScope(sessionJob+Dispatchers.Main)
                repository=ItemRepository(database,CloudApi(this@DormSwapApp,client,auth,id),user,
                    network,AppPreferences(this@DormSwapApp,id),scope)
                _readyUser.value=id
                try { network.isOnline.collect { if(it) repository.requestSync() } }
                finally {
                    _readyUser.value=null
                    withContext(NonCancellable) {
                        sessionJob.cancelAndJoin()
                        withContext(Dispatchers.IO) { database.close() }
                    }
                }
            }
        }
    }
}
