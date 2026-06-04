package com.lixiangyang.talky.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthApiClient(
    private val baseUrls: List<String> = DEFAULT_BASE_URLS
) {
    suspend fun login(username: String, password: String): ApiResult<AuthResponse> {
        return post(
            path = "/api/auth/login",
            body = JSONObject()
                .put("username", username)
                .put("password", password)
        )
    }

    suspend fun register(username: String, password: String, nickname: String? = null): ApiResult<AuthResponse> {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
        if (!nickname.isNullOrBlank()) {
            body.put("nickname", nickname)
        }
        return post(path = "/api/auth/register", body = body)
    }

    private suspend fun post(path: String, body: JSONObject): ApiResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            var lastNetworkError: String? = null

            for (baseUrl in baseUrls) {
                val result = runCatching {
                    postOnce(baseUrl = baseUrl.trimEnd('/'), path = path, body = body)
                }.getOrElse { error ->
                    lastNetworkError = error.message ?: "网络连接失败"
                    null
                }

                when (result) {
                    is ApiResult.Success -> return@withContext result
                    is ApiResult.Error -> return@withContext result
                    null -> Unit
                }
            }

            ApiResult.Error(lastNetworkError ?: "无法连接服务器，请确认后端服务已启动")
        }
    }

    private fun postOnce(baseUrl: String, path: String, body: JSONObject): ApiResult<AuthResponse> {
        val url = "$baseUrl$path"
        Log.d(TAG, "POST $url")
        Log.d(TAG, "Request: ${body.toSafeLogBody()}")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseText = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            Log.d(TAG, "Response: HTTP $statusCode $responseText")

            if (statusCode in 200..299) {
                ApiResult.Success(parseAuthResponse(responseText))
            } else {
                ApiResult.Error(parseErrorMessage(responseText, statusCode))
            }
        } catch (error: Exception) {
            Log.e(TAG, "Request failed: POST $url", error)
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toSafeLogBody(): String {
        val safeBody = JSONObject(toString())
        if (safeBody.has("password")) {
            safeBody.put("password", "******")
        }
        return safeBody.toString()
    }

    private fun parseAuthResponse(responseText: String): AuthResponse {
        val json = JSONObject(responseText)
        val userJson = json.getJSONObject("user")
        return AuthResponse(
            token = json.getString("token"),
            user = UserResponse(
                id = userJson.getLong("id"),
                username = userJson.getString("username"),
                nickname = userJson.optString("nickname", userJson.getString("username")),
                createdAt = userJson.optString("createdAt", "")
            )
        )
    }

    private fun parseErrorMessage(responseText: String, statusCode: Int): String {
        if (responseText.isBlank()) {
            return "请求失败：HTTP $statusCode"
        }

        val json = runCatching { JSONObject(responseText) }.getOrNull()
            ?: return "请求失败：HTTP $statusCode"

        val fields = json.optJSONObject("fields")
        if (fields != null && fields.length() > 0) {
            val firstKey = fields.keys().asSequence().firstOrNull()
            val firstMessage = firstKey?.let { fields.optString(it) }
            if (!firstMessage.isNullOrBlank()) {
                return translateServerMessage(firstMessage)
            }
        }

        val code = json.optString("code")
        val message = json.optString("message")
        return translateServerMessage(message.ifBlank { code.ifBlank { "请求失败：HTTP $statusCode" } })
    }

    private fun translateServerMessage(message: String): String {
        return when (message) {
            "Username already exists" -> "账号已存在"
            "Invalid username or password" -> "账号或密码不正确"
            "Request validation failed" -> "账号或密码格式不正确"
            "must contain only letters, numbers, and underscores" -> "账号只能包含字母、数字和下划线"
            "size must be between 3 and 50" -> "账号长度需要 3 到 50 位"
            "size must be between 6 and 64" -> "密码长度需要 6 到 64 位"
            else -> message
        }
    }

    data class AuthResponse(
        val token: String,
        val user: UserResponse
    )

    data class UserResponse(
        val id: Long,
        val username: String,
        val nickname: String,
        val createdAt: String
    )

    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String) : ApiResult<Nothing>()
    }

    companion object {
        private const val TAG = "TalkyAuthApi"
        private const val TIMEOUT_MS = 5000

        private val DEFAULT_BASE_URLS = listOf("http://127.0.0.1:8082")
    }
}
