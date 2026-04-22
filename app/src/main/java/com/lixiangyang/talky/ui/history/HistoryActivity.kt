package com.lixiangyang.talky.ui.history

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.lixiangyang.talky.ui.common.BaseFragmentHostActivity

class HistoryActivity : BaseFragmentHostActivity() {
    private var hasLaunchedAutoplay = false

    override fun createFragment(): Fragment {
        val autoplayPracticeId = intent.getLongExtra(EXTRA_AUTOPLAY_PRACTICE_ID, 0L)
        return HistoryFragment().apply {
            arguments = bundleOf(EXTRA_AUTOPLAY_PRACTICE_ID to autoplayPracticeId)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        val autoplayPracticeId = intent.getLongExtra(EXTRA_AUTOPLAY_PRACTICE_ID, 0L)
        if (autoplayPracticeId > 0L && !hasLaunchedAutoplay) {
            hasLaunchedAutoplay = true
            startActivity(com.lixiangyang.talky.ui.video.VideoPlayerActivity.createIntent(this, autoplayPracticeId))
        }
    }

    companion object {
        private const val EXTRA_AUTOPLAY_PRACTICE_ID = "extra_autoplay_practice_id"

        fun createIntent(context: Context, autoplayPracticeId: Long? = null): Intent =
            Intent(context, HistoryActivity::class.java).apply {
                autoplayPracticeId?.takeIf { it > 0 }?.let {
                    putExtra(EXTRA_AUTOPLAY_PRACTICE_ID, it)
                }
            }
    }
}
