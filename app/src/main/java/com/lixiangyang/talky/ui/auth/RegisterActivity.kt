package com.lixiangyang.talky.ui.auth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.lixiangyang.talky.core.AuthManager
import com.lixiangyang.talky.databinding.ActivityRegisterBinding
import com.lixiangyang.talky.ui.recording.RecordingActivity

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
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

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        openRecordingAfterLogin = intent.getBooleanExtra(EXTRA_OPEN_RECORDING_AFTER_LOGIN, false)

        binding.backButton.setOnClickListener { finish() }
        binding.registerButton.setOnClickListener { register() }
        binding.loginEntry.setOnClickListener { finish() }
    }

    private fun register() {
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirmPassword = binding.confirmPasswordInput.text?.toString().orEmpty()
        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        val result = authManager.register(
            account = binding.accountInput.text?.toString().orEmpty(),
            password = password
        )

        when (result) {
            is AuthManager.AuthResult.Success -> {
                Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
                continueAfterAuth()
            }
            is AuthManager.AuthResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
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
        ): Intent = Intent(context, RegisterActivity::class.java).apply {
            putExtra(EXTRA_OPEN_RECORDING_AFTER_LOGIN, openRecordingAfterLogin)
        }
    }
}
