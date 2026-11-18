package com.example.myapplication.data.auth

import com.example.myapplication.data.remote.ApiException
import com.example.myapplication.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

data class AuthUser(val id: String, val email: String, val name: String)

class AuthRepository(private val client: SupabaseClient, private val store: SessionStore) {
    private var session: JSONObject? = store.read()?.let { runCatching { JSONObject(it) }.getOrNull() }
    private val _user = MutableStateFlow(session?.let { runCatching { userFrom(it) }.getOrNull() })
    val user: StateFlow<AuthUser?> = _user
    private val mutex = Mutex()

    private fun userFrom(json: JSONObject): AuthUser {
        val u = json.getJSONObject("user")
        return AuthUser(u.getString("id"), u.getString("email"), u.optJSONObject("user_metadata")?.optString("display_name") ?: "Student")
    }
    suspend fun signIn(email: String, password: String, name: String? = null) = mutex.withLock {
        AuthValidator.validate(email, password, name)?.let { throw IOException(it) }
        val body = JSONObject().put("email", AuthValidator.normalizeEmail(email)).put("password", password)
        if (name != null) body.put("data", JSONObject().put("display_name", name.trim()))
        val path = if (name == null) "/auth/v1/token?grant_type=password" else "/auth/v1/signup"
        val result = JSONObject(client.json(path, body))
        if (!result.has("access_token")) throw IOException("Email confirmation is enabled on the server. Disable Confirm email in Supabase before using this demo.")
        save(result)
    }
    private suspend fun save(result: JSONObject) {
        if (!result.has("expires_at")) result.put("expires_at", System.currentTimeMillis()/1000 + result.optLong("expires_in",3600))
        withContext(Dispatchers.IO) { store.write(result.toString()) }
        session = result
        _user.value = userFrom(result)
    }
    suspend fun token(): String = mutex.withLock {
        var s = session ?: throw IOException("Please sign in again.")
        if (s.optLong("expires_at") < System.currentTimeMillis()/1000 + 60) {
            try {
                s = JSONObject(client.json("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token",s.getString("refresh_token"))))
                save(s)
            } catch (e: ApiException) {
                if (e.status == 400 || e.status == 401) { store.clear(); session=null; _user.value=null }
                throw e
            }
        }
        s.getString("access_token")
    }
    suspend fun signOut() = mutex.withLock {
        // Local sign out also works offline; Supabase access tokens expire after their normal lifetime.
        withContext(Dispatchers.IO) { store.clear() }
        session=null
        _user.value=null
    }
}
