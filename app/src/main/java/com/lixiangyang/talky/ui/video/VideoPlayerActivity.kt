package com.lixiangyang.talky.ui.video

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.TimeBar
import com.lixiangyang.talky.TalkyApp
import com.lixiangyang.talky.databinding.ActivityVideoPlayerBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Formatter
import java.util.Locale

class VideoPlayerActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_PRACTICE_ID = "extra_practice_id"
        private const val AUTO_HIDE_DELAY_MS = 3_000L

        fun createIntent(context: Context, practiceId: Long): Intent =
            Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_PRACTICE_ID, practiceId)
            }
    }

    private lateinit var binding: ActivityVideoPlayerBinding

    private val practiceId: Long by lazy { intent.getLongExtra(EXTRA_PRACTICE_ID, 0L) }
    private val viewModel: VideoDetailViewModel by viewModels {
        VideoDetailViewModel.factory((application as TalkyApp).container.repository, practiceId)
    }

    private var player: ExoPlayer? = null
    private var controlsVisible = true
    private var isScrubbing = false
    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, 500)
        }
    }
    private val controlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        if (!isScrubbing) {
            hideControls()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlayPauseUi()
            updateProgress()
            if (playbackState == Player.STATE_READY && player?.isPlaying == true) {
                scheduleAutoHide()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseUi()
            if (isPlaying) {
                scheduleAutoHide()
            } else {
                showControls(autoHide = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets()

        binding.backButton.setOnClickListener { finish() }
        setupControls()

        viewModel.practice.observe(this) { practice ->
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            binding.title.text = practice.title
            binding.meta.text = "${formatter.format(Date(practice.recordedAt))} · ${practice.durationSeconds}秒 · ${practice.resolution}"
            initializePlayer(practice.filePath)
        }
    }

    private fun setupControls() {
        binding.playerContainer.setOnClickListener {
            toggleControls()
        }

        binding.playPauseButton.setOnClickListener {
            val activePlayer = player ?: return@setOnClickListener
            if (activePlayer.isPlaying) {
                activePlayer.pause()
            } else {
                if (activePlayer.playbackState == Player.STATE_ENDED) {
                    activePlayer.seekTo(0)
                }
                activePlayer.play()
            }
            updatePlayPauseUi()
            showControls()
        }

        binding.timeBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                isScrubbing = true
                controlsHandler.removeCallbacks(hideControlsRunnable)
                showControls(autoHide = false)
                binding.position.text = formatDuration(position)
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                binding.position.text = formatDuration(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                isScrubbing = false
                if (!canceled) {
                    player?.seekTo(position)
                }
                updateProgress()
                scheduleAutoHide()
            }
        })
    }

    private fun initializePlayer(filePath: String) {
        if (player != null) return

        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.addListener(playerListener)

            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                Uri.parse("file://$filePath")
            }

            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.playWhenReady = true
            exo.prepare()
            updatePlayPauseUi()
            progressHandler.post(progressRunnable)
            showControls()
        }
    }

    private fun updatePlayPauseUi() {
        val isPlaying = player?.isPlaying == true
        binding.playPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun updateProgress() {
        val activePlayer = player ?: return
        val position = activePlayer.currentPosition.coerceAtLeast(0L)
        val duration = activePlayer.duration.takeIf { it > 0 } ?: 0L
        binding.timeBar.setDuration(duration)
        binding.timeBar.setPosition(position)
        binding.timeBar.setBufferedPosition(activePlayer.bufferedPosition)
        binding.position.text = formatDuration(position)
        binding.duration.text = formatDuration(duration)
    }

    private fun formatDuration(durationMs: Long): String {
        formatBuilder.setLength(0)
        return Util.getStringForTime(formatBuilder, formatter, durationMs)
    }

    private fun applyEdgeToEdgeInsets() {
        val topStart = binding.topOverlay.paddingStart
        val topTop = binding.topOverlay.paddingTop
        val topEnd = binding.topOverlay.paddingEnd
        val topBottom = binding.topOverlay.paddingBottom
        val controlsStart = binding.bottomControls.paddingStart
        val controlsTop = binding.bottomControls.paddingTop
        val controlsEnd = binding.bottomControls.paddingEnd
        val controlsBottom = binding.bottomControls.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.playerScreen) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topOverlay.updatePadding(
                left = topStart + bars.left,
                top = topTop + bars.top,
                right = topEnd + bars.right,
                bottom = topBottom
            )
            binding.bottomControls.updatePadding(
                left = controlsStart + bars.left,
                top = controlsTop,
                right = controlsEnd + bars.right,
                bottom = controlsBottom + bars.bottom
            )
            insets
        }
    }

    private fun toggleControls() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls(autoHide: Boolean = true) {
        controlsVisible = true
        binding.bottomControls.visibility = View.VISIBLE
        if (autoHide && player?.isPlaying == true) {
            scheduleAutoHide()
        } else {
            controlsHandler.removeCallbacks(hideControlsRunnable)
        }
    }

    private fun hideControls() {
        controlsVisible = false
        controlsHandler.removeCallbacks(hideControlsRunnable)
        binding.bottomControls.visibility = View.GONE
    }

    private fun scheduleAutoHide() {
        controlsHandler.removeCallbacks(hideControlsRunnable)
        if (player?.isPlaying == true && !isScrubbing) {
            controlsHandler.postDelayed(hideControlsRunnable, AUTO_HIDE_DELAY_MS)
        }
    }

    override fun onStart() {
        super.onStart()
        progressHandler.post(progressRunnable)
    }

    override fun onStop() {
        super.onStop()
        progressHandler.removeCallbacks(progressRunnable)
        controlsHandler.removeCallbacks(hideControlsRunnable)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        controlsHandler.removeCallbacks(hideControlsRunnable)
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }
}
