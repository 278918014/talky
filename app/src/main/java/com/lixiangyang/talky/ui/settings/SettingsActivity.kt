package com.lixiangyang.talky.ui.settings

import androidx.fragment.app.Fragment
import com.lixiangyang.talky.R
import com.lixiangyang.talky.ui.common.BaseBottomNavActivity

class SettingsActivity : BaseBottomNavActivity() {
    override val selectedMenuItemId: Int = R.id.settingsFragment

    override fun createFragment(): Fragment = SettingsFragment()
}
