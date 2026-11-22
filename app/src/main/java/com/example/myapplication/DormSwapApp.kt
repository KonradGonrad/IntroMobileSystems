package com.example.myapplication

import android.app.Application
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.data.auth.*
import com.example.myapplication.data.local.*
import com.example.myapplication.data.photo.PhotoStorage
import com.example.myapplication.data.remote.*
import com.example.myapplication.data.repository.ItemRepository
import com.example.myapplication.data.notifications.*
import org.json.JSONObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DormSwapApp : Application() {
    val appScope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
    lateinit var auth: AuthRepository; private set
    lateinit var repository: ItemRepository; private set
    lateinit var photoStorage: PhotoStorage; private set
    private val _readyUser=MutableStateFlow<String?>(null)
    val readyUser: StateFlow<String?> = _readyUser
    val foreground = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        val client=SupabaseClient()
        auth=AuthRepository(client,SessionStore(this))
        photoStorage=PhotoStorage(this)
        PushRegistration.initialize(this)
        val network=NetworkMonitor(this)
        appScope.launch {
            auth.user.map { it?.id }.distinctUntilChanged().collectLatest { id ->
                _readyUser.value=null
                if(id==null) return@collectLatest
                val user=auth.user.value ?: return@collectLatest
                val database=UserDatabase.create(this@DormSwapApp,id)
                val sessionJob=SupervisorJob()
                val scope=CoroutineScope(sessionJob+Dispatchers.Main)
                val api = CloudApi(this@DormSwapApp,client,auth,id)
                repository=ItemRepository(database,api,user,
                    network,AppPreferences(this@DormSwapApp,id),scope,ActivityInbox(this@DormSwapApp,id))
                scope.launch {
                    combine(network.isOnline, PushRegistration.token) { online, token -> token.takeIf { online } }
                        .collectLatest { token ->
                            if (token != null) while (isActive) {
                                try { api.rpc("register_device", JSONObject().put("device_token",token)); break }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { delay(60_000) }
                            }
                        }
                }
                _readyUser.value=id
                try {
                    combine(network.isOnline, foreground) { online, visible -> online && visible }
                        .distinctUntilChanged().collectLatest { active ->
                            if (active) while (currentCoroutineContext().isActive) {
                                repository.sync()
                                delay(5_000)
                            }
                        }
                }
                finally {
                    _readyUser.value=null
                    withContext(NonCancellable) {
                        sessionJob.cancelAndJoin()
                        getSystemService(android.app.NotificationManager::class.java).cancelAll()
                        withContext(Dispatchers.IO) { database.close() }
                    }
                }
            }
        }
    }
}
