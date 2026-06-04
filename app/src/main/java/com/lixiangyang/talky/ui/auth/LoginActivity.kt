package com.lixiangyang.talky.ui.auth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.lixiangyang.talky.core.AuthManager
import com.lixiangyang.talky.databinding.ActivityLoginBinding
import com.lixiangyang.talky.ui.recording.RecordingActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    private var openRecordingAfterLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        openRecordingAfterLogin = intent.getBooleanExtra(EXTRA_OPEN_RECORDING_AFTER_LOGIN, false)

        binding.backButton.setOnClickListener { finish() }
        binding.loginButton.setOnClickListener { login() }
        binding.registerEntry.setOnClickListener {
            startActivity(RegisterActivity.createIntent(this))
        }
    }

    private fun login() {
        setLoading(true)
        lifecycleScope.launch {
            val result = authManager.login(
                account = binding.accountInput.text?.toString().orEmpty(),
                password = binding.passwordInput.text?.toString().orEmpty()
            )
            setLoading(false)

            when (result) {
                is AuthManager.AuthResult.Success -> {
                    Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    continueAfterAuth()
                }
                is AuthManager.AuthResult.Error -> {
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.registerEntry.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) "登录中..." else "登录"
    }

    private fun continueAfterAuth() {
        if (openRecordingAfterLogin) {
            startActivity(RecordingActivity.createIntent(this))
        }
        finish()
    }

    companion object {
        private const val EXTRA_OPEN_RECORDING_AFTER_LOGIN = "open_recording_after_login"

        fun createIntent(
            context: Context,
            openRecordingAfterLogin: Boolean = false
        ): Intent = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_OPEN_RECORDING_AFTER_LOGIN, openRecordingAfterLogin)
        }
    }
}
