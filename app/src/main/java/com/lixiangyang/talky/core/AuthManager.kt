package com.lixiangyang.talky.core

import android.content.Context
import com.lixiangyang.talky.data.remote.AuthApiClient

class AuthManager(
    context: Context,
    private val authApiClient: AuthApiClient = AuthApiClient()
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "talky_auth",
        Context.MODE_PRIVATE
    )

    val isLoggedIn: Boolean
        get() = preferences.getBoolean(KEY_LOGGED_IN, false) &&
            preferences.getString(KEY_CURRENT_ACCOUNT, null).isNullOrBlank().not() &&
            preferences.getString(KEY_TOKEN, null).isNullOrBlank().not()

    val currentAccount: String?
        get() = preferences.getString(KEY_CURRENT_ACCOUNT, null)

    suspend fun register(account: String, password: String): AuthResult {
        val normalizedAccount = account.trim()
        val validation = validateCredentials(normalizedAccount, password)
        if (validation != null) return validation

        return when (val result = authApiClient.register(normalizedAccount, password)) {
            is AuthApiClient.ApiResult.Success -> {
                AuthResult.Success
            }
            is AuthApiClient.ApiResult.Error -> AuthResult.Error(result.message)
        }
    }

    suspend fun login(account: String, password: String): AuthResult {
        val normalizedAccount = account.trim()
        val validation = validateCredentials(normalizedAccount, password)
        if (validation != null) return validation

        return when (val result = authApiClient.login(normalizedAccount, password)) {
            is AuthApiClient.ApiResult.Success -> {
                saveSession(result.data)
                AuthResult.Success
            }
            is AuthApiClient.ApiResult.Error -> AuthResult.Error(result.message)
        }
    }

    fun logout() {
        preferences.edit()
            .remove(KEY_CURRENT_ACCOUNT)
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_NICKNAME)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    private fun validateCredentials(account: String, password: String): AuthResult.Error? {
        return when {
            account.length < 3 -> AuthResult.Error("账号至少需要 3 位")
            !account.matches(Regex("^[a-zA-Z0-9_]+$")) -> AuthResult.Error("账号只能包含字母、数字和下划线")
            password.length < 6 -> AuthResult.Error("密码至少需要 6 位")
            else -> null
        }
    }

    private fun saveSession(response: AuthApiClient.AuthResponse) {
        preferences.edit()
            .putString(KEY_TOKEN, response.token)
            .putLong(KEY_USER_ID, response.user.id)
            .putString(KEY_CURRENT_ACCOUNT, response.user.username)
            .putString(KEY_NICKNAME, response.user.nickname)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_CURRENT_ACCOUNT = "current_account"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}
