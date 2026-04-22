package com.lixiangyang.talky.ui.common

import androidx.fragment.app.Fragment
import com.lixiangyang.talky.TalkyApp
import com.lixiangyang.talky.core.AppContainer

fun Fragment.container(): AppContainer = (requireActivity().application as TalkyApp).container
