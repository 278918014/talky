package com.lixiangyang.talky.core

import android.content.Context
import java.security.MessageDigest

class AuthManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "talky_auth",
        Context.MODE_PRIVATE
    )

    val isLoggedIn: Boolean
        get() = preferences.getBoolean(KEY_LOGGED_IN, false) &&
            preferences.getString(KEY_CURRENT_ACCOUNT, null).isNullOrBlank().not()

    val currentAccount: String?
        get() = preferences.getString(KEY_CURRENT_ACCOUNT, null)

    fun hasRegisteredAccount(): Boolean {
        return preferences.getString(KEY_ACCOUNT, null).isNullOrBlank().not()
    }

    fun register(account: String, password: String): AuthResult {
        val normalizedAccount = account.trim()
        val validation = validateCredentials(normalizedAccount, password)
        if (validation != null) return validation

        preferences.edit()
            .putString(KEY_ACCOUNT, normalizedAccount)
            .putString(KEY_PASSWORD_HASH, hashPassword(password))
            .putString(KEY_CURRENT_ACCOUNT, normalizedAccount)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        return AuthResult.Success
    }

    fun login(account: String, password: String): AuthResult {
        val normalizedAccount = account.trim()
        val validation = validateCredentials(normalizedAccount, password)
        if (validation != null) return validation

        val savedAccount = preferences.getString(KEY_ACCOUNT, null)
        val savedHash = preferences.getString(KEY_PASSWORD_HASH, null)
        if (savedAccount.isNullOrBlank() || savedHash.isNullOrBlank()) {
            return AuthResult.Error("还没有账号，请先注册")
        }
        if (normalizedAccount != savedAccount || hashPassword(password) != savedHash) {
            return AuthResult.Error("账号或密码不正确")
        }

        preferences.edit()
            .putString(KEY_CURRENT_ACCOUNT, normalizedAccount)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        return AuthResult.Success
    }

    fun logout() {
        preferences.edit()
            .remove(KEY_CURRENT_ACCOUNT)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    private fun validateCredentials(account: String, password: String): AuthResult.Error? {
        return when {
            account.length < 3 -> AuthResult.Error("账号至少需要 3 位")
            password.length < 8 -> AuthResult.Error("密码至少需要 8 位")
            else -> null
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val KEY_ACCOUNT = "account"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_CURRENT_ACCOUNT = "current_account"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}
