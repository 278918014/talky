package com.lixiangyang.talky.ui.common

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.lixiangyang.talky.R

abstract class BaseFragmentHostActivity : AppCompatActivity() {
    protected open val layoutResId: Int = R.layout.activity_fragment_host
    protected open val fragmentContainerId: Int = R.id.fragment_container

    protected abstract fun createFragment(): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(layoutResId)
        applyEdgeToEdgeInsets()

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(fragmentContainerId, createFragment())
            }
        }
    }

    private fun applyEdgeToEdgeInsets() {
        val fragmentContainer = findViewById<View>(fragmentContainerId) ?: return
        val bottomNav = findViewById<View?>(R.id.bottom_nav)
        val containerTop = fragmentContainer.paddingTop
        val containerBottom = fragmentContainer.paddingBottom
        val bottomNavBottom = bottomNav?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            fragmentContainer.updatePadding(
                top = containerTop + bars.top,
                bottom = containerBottom + if (bottomNav == null) bars.bottom else 0
            )
            bottomNav?.updatePadding(bottom = bottomNavBottom + bars.bottom)
            insets
        }
    }
}
