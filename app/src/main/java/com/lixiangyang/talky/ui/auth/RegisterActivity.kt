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
import com.lixiangyang.talky.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager

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

        setLoading(true)
        lifecycleScope.launch {
            val result = authManager.register(
                account = binding.accountInput.text?.toString().orEmpty(),
                password = password
            )
            setLoading(false)

            when (result) {
                is AuthManager.AuthResult.Success -> {
                    Toast.makeText(this@RegisterActivity, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AuthManager.AuthResult.Error -> {
                    Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.registerButton.isEnabled = !isLoading
        binding.loginEntry.isEnabled = !isLoading
        binding.registerButton.text = if (isLoading) "注册中..." else "注册并登录"
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, RegisterActivity::class.java)
    }
}
