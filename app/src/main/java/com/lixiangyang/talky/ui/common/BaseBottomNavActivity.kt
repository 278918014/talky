package com.lixiangyang.talky.ui.common

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lixiangyang.talky.MainActivity
import com.lixiangyang.talky.R
import com.lixiangyang.talky.ui.settings.SettingsActivity

abstract class BaseBottomNavActivity : BaseFragmentHostActivity() {
    override val layoutResId: Int = R.layout.activity_bottom_nav_host

    protected abstract val selectedMenuItemId: Int
    private var lastHomeBackPressedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBottomNav()
        setupBackNavigation()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = selectedMenuItemId
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedMenuItemId) {
                true
            } else {
                val targetIntent = when (item.itemId) {
                    R.id.homeFragment -> Intent(this, MainActivity::class.java)
                    R.id.settingsFragment -> Intent(this, SettingsActivity::class.java)
                    else -> null
                }

                if (targetIntent != null) {
                    targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
                    startActivity(targetIntent, options.toBundle())
                    finish()
                }
                true
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (selectedMenuItemId != R.id.homeFragment) {
                        navigateToHome()
                        return
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastHomeBackPressedAt <= EXIT_INTERVAL_MS) {
                        finish()
                    } else {
                        lastHomeBackPressedAt = now
                        Toast.makeText(this@BaseBottomNavActivity, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun navigateToHome() {
        val targetIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
        startActivity(targetIntent, options.toBundle())
        finish()
    }

    companion object {
        private const val EXIT_INTERVAL_MS = 2_000L
    }
}
