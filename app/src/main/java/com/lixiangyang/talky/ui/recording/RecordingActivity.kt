package com.lixiangyang.talky.ui.recording

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.lixiangyang.talky.ui.common.BaseFragmentHostActivity

class RecordingActivity : BaseFragmentHostActivity() {
    override fun createFragment(): Fragment = RecordingFragment()

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, RecordingActivity::class.java)
    }
}
