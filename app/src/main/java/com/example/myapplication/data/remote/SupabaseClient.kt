package com.example.myapplication.data.remote

import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val status: Int, message: String) : IOException(message)

/** Hosted connections use HTTPS. Debug builds can also reach a local server via adb reverse. */
class SupabaseClient {
    val configured get() = BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && (
        BuildConfig.SUPABASE_URL.startsWith("https://") ||
            (BuildConfig.DEBUG && Regex("""http://127\.0\.0\.1:[0-9]{1,5}/?""").matches(BuildConfig.SUPABASE_URL))
    )
    suspend fun request(path: String, method: String = "GET", token: String? = null,
                        body: ByteArray? = null, contentType: String = "application/json"): ByteArray = withContext(Dispatchers.IO) {
        if (!configured) throw IOException("Supabase is not configured. Follow M3/backend/README.md.")
        val connection = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", contentType)
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val raw = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = try {
                    val json = JSONObject(raw)
                    json.optString("msg").ifBlank { json.optString("message") }.ifBlank { json.optString("error_description") }
                } catch (_: Exception) { "" }
                throw ApiException(code, message.ifBlank { "Server returned HTTP $code" }.take(240))
            }
            connection.inputStream.use { it.readBytes() }
        } finally { connection.disconnect() }
    }
    suspend fun json(path: String, body: JSONObject, token: String? = null): String =
        request(path, "POST", token, body.toString().toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
}
