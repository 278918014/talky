package com.lixiangyang.talky

import androidx.fragment.app.Fragment
import com.lixiangyang.talky.ui.common.BaseBottomNavActivity
import com.lixiangyang.talky.ui.home.HomeFragment

class MainActivity : BaseBottomNavActivity() {
    override val selectedMenuItemId: Int = R.id.homeFragment

    override fun createFragment(): Fragment = HomeFragment()
}
