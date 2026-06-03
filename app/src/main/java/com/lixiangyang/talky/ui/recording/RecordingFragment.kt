package com.lixiangyang.talky.ui.recording

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lixiangyang.talky.core.AppSettings
import com.lixiangyang.talky.databinding.FragmentRecordingBinding
import com.lixiangyang.talky.ui.common.VideoThumbnailLoader
import com.lixiangyang.talky.ui.common.container
import com.lixiangyang.talky.ui.history.HistoryActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordingFragment : Fragment() {
    companion object {
        private const val TAG = "TalkyRecording"
    }

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordingViewModel by viewModels {
        RecordingViewModel.factory(container().repository)
    }

    // CameraX
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private lateinit var cameraExecutor: ExecutorService
    private var isCameraReady = false
    private var isStartingRecording = false

    // Timer
    private var elapsedSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            updateTimerDisplay()
            handler.postDelayed(this, 1000)
        }
    }

    // Recording state
    private var isRecording = false
    private var isRecordingStarted = false

    private var isPaused = false
    private var shouldSaveCurrentRecording = true
    private var finishAfterFinalize = false

    // Permission
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        if (allGranted) {
            startCamera()
            // Don't auto-start recording, wait for user to click "开始"
        } else {
            Snackbar.make(binding.root, "需要摄像头和麦克风权限才能录制", Snackbar.LENGTH_LONG).show()
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        observeState()

        // Initialize camera preview only, don't auto-start recording
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startCamera()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun setupUI() {
        // Initialize button text
        binding.pauseButton.text = "开始"
        binding.stopButton.isEnabled = false

        binding.backButton.setOnClickListener {
            if (recording != null || isRecordingStarted) {
                stopRecording(save = false)
            } else {
                requireActivity().finish()
            }
        }

        binding.switchCameraButton.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            startCamera()
        }

        binding.settingsButton.setOnClickListener {
            showRecordingSettingsDialog()
        }

        binding.pauseButton.setOnClickListener {
            if (!isRecordingStarted) {
                // Start recording
                requestPermissionsAndStart()
            } else if (isPaused) {
                resumeRecording()
            } else {
                pauseRecording()
            }
        }

        binding.stopButton.setOnClickListener {
            stopRecording(save = true)
        }

    }

    private fun showRecordingSettingsDialog() {
        val resolution = AppSettings.getResolution(requireContext()).label
        val storagePath = getVideoStoragePath()
        val options = arrayOf(
            "默认录制分辨率\n$resolution",
            "视频保存位置\n$storagePath"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("录制设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showResolutionDialog()
                    1 -> showStoragePathDialog(storagePath)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showResolutionDialog() {
        if (isRecordingStarted || isStartingRecording || recording != null) {
            Snackbar.make(binding.root, "录制中不能修改分辨率，请先停止当前录制", Snackbar.LENGTH_SHORT).show()
            return
        }

        val options = AppSettings.resolutionOptions
        val labels = options.map { it.label }.toTypedArray()
        val checkedIndex = AppSettings.getResolutionIndex(requireContext())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("默认录制分辨率")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selected = options[which]
                AppSettings.setResolution(requireContext(), selected)
                startCamera()
                Snackbar.make(binding.root, "已设置为 ${selected.label}", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showStoragePathDialog(storagePath: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("视频保存位置")
            .setMessage("当前视频会保存到：\n\n$storagePath")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun getVideoStoragePath(): String {
        return getVideoStorageDirectory().absolutePath
    }

    private fun getVideoStorageDirectory(): File {
        return File(Environment.getExternalStorageDirectory(), "Talky")
    }

    private fun observeState() {
        viewModel.saveDone.observe(viewLifecycleOwner) { practiceId ->
            if (practiceId != null && practiceId > 0) {
                Snackbar.make(binding.root, "视频已保存", Snackbar.LENGTH_SHORT).show()
                viewModel.onSaveHandled()
                startActivity(HistoryActivity.createIntent(requireContext(), practiceId))
                requireActivity().finish()
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            if (!hasRootStorageAccess()) {
                showRootStorageAccessDialog()
                return
            }
            // 如果相机还没准备好，先初始化相机，相机就绪后再开始录制
            if (!isCameraReady) {
                startCamera { startRecording() }
            } else {
                startRecording()
            }
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startCamera(onReady: (() -> Unit)? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val selectedResolution = AppSettings.getResolution(requireContext())
            val selectedQuality = selectedResolution.toCameraQuality()
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        selectedQuality,
                        FallbackStrategy.lowerQualityOrHigherThan(selectedQuality)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                binding.cameraPlaceholder.visibility = View.GONE
                isCameraReady = true
                onReady?.invoke()
            } catch (e: Exception) {
                binding.cameraPlaceholder.visibility = View.VISIBLE
                isCameraReady = false
                Log.e(TAG, "startCamera failed", e)
                Snackbar.make(binding.root, "相机启动失败：${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun AppSettings.ResolutionOption.toCameraQuality(): Quality {
        return when (key) {
            "sd" -> Quality.SD
            "fhd" -> Quality.FHD
            else -> Quality.HD
        }
    }

    private fun startRecording() {
        if (isStartingRecording || recording != null) {
            return
        }
        val videoCapture = this.videoCapture ?: run {
            Snackbar.make(binding.root, "相机未初始化，请稍候", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Explicitly check audio permission before enabling audio
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            Snackbar.make(binding.root, "缺少麦克风权限，无法录制音频", Snackbar.LENGTH_LONG).show()
            return
        }

        if (!hasRootStorageAccess()) {
            showRootStorageAccessDialog()
            return
        }

        val name = "Talky_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.mp4"
        val storageDir = getVideoStorageDirectory()
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Snackbar.make(binding.root, "无法创建保存目录：${storageDir.absolutePath}", Snackbar.LENGTH_LONG).show()
            return
        }
        val outputFile = File(storageDir, name)

        val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()

        try {
            isStartingRecording = true
            shouldSaveCurrentRecording = true
            finishAfterFinalize = false
            elapsedSeconds = 0
            updateTimerDisplay()
            binding.pauseButton.isEnabled = false
            binding.pauseButton.text = "准备中..."
            binding.stopButton.isEnabled = false
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            recording = videoCapture.output
                .prepareRecording(requireContext(), fileOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                    if (_binding == null) {
                        if (event is VideoRecordEvent.Finalize && (!shouldSaveCurrentRecording || event.hasError())) {
                            cleanupFailedRecording(event.outputResults.outputUri)
                        }
                        return@start
                    }
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            handler.post(timerRunnable)
                            binding.pauseButton.isEnabled = true
                            binding.pauseButton.text = "暂停"
                            binding.stopButton.isEnabled = false
                            isRecording = true
                            isRecordingStarted = true
                            isStartingRecording = false
                            isPaused = false
                        }
                        is VideoRecordEvent.Status -> {
                            if (isRecordingStarted && !isPaused) {
                                val hasRecordedData =
                                    event.recordingStats.recordedDurationNanos > 0L ||
                                        event.recordingStats.numBytesRecorded > 0L
                                if (hasRecordedData && !binding.stopButton.isEnabled) {
                                    binding.stopButton.isEnabled = true
                                }
                            }
                        }
                        is VideoRecordEvent.Pause -> {
                            binding.stopButton.isEnabled = elapsedSeconds > 0
                        }
                        is VideoRecordEvent.Resume -> {
                            if (elapsedSeconds > 0) {
                                binding.stopButton.isEnabled = true
                            }
                        }
                        is VideoRecordEvent.Finalize -> {
                            isStartingRecording = false
                            handler.removeCallbacks(timerRunnable)
                            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            val shouldSave = shouldSaveCurrentRecording
                            if (!shouldSave) {
                                cleanupFailedRecording(event.outputResults.outputUri)
                                resetRecordingUi()
                                Snackbar.make(binding.root, "录制已取消", Snackbar.LENGTH_SHORT).show()
                                val shouldFinish = finishAfterFinalize
                                finishAfterFinalize = false
                                if (shouldFinish) {
                                    requireActivity().finish()
                                }
                                return@start
                            }
                            if (event.hasError()) {
                                cleanupFailedRecording(event.outputResults.outputUri)
                                val fullMsg = buildFinalizeErrorMessage(event)
                                Snackbar.make(binding.root, fullMsg, Snackbar.LENGTH_LONG).show()
                                resetRecordingUi()
                            } else {
                                val outputUri = event.outputResults.outputUri
                                if (outputUri == android.net.Uri.EMPTY || outputUri.toString().isBlank()) {
                                    Snackbar.make(binding.root, "录制完成了，但没有拿到有效的视频地址", Snackbar.LENGTH_LONG).show()
                                    resetRecordingUi()
                                    return@start
                                }
                                val durationSeconds = elapsedSeconds
                                val thumbnailPath = VideoThumbnailLoader.generateThumbnailFile(
                                    requireContext(),
                                    outputUri.toString()
                                )
                                viewModel.savePractice(
                                    title = name.removeSuffix(".mp4"),
                                    durationSeconds = durationSeconds,
                                    resolution = AppSettings.getResolution(requireContext()).recordingLabel,
                                    filePath = outputFile.absolutePath,
                                    thumbnailPath = thumbnailPath
                                )
                                resetRecordingUi()
                            }
                        }
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "startRecording security exception", e)
            isStartingRecording = false
            binding.pauseButton.isEnabled = true
            binding.pauseButton.text = "开始"
            binding.stopButton.isEnabled = false
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Snackbar.make(binding.root, "麦克风权限被拒绝，请到设置中开启", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            isStartingRecording = false
            binding.pauseButton.isEnabled = true
            binding.pauseButton.text = "开始"
            binding.stopButton.isEnabled = false
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            Snackbar.make(binding.root, "录制启动失败：$detail", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun pauseRecording() {
        recording?.pause()
        handler.removeCallbacks(timerRunnable)
        binding.pauseButton.text = "继续"
        isPaused = true
        isRecording = false
    }

    private fun resumeRecording() {
        recording?.resume()
        handler.post(timerRunnable)
        binding.pauseButton.text = "暂停"
        isPaused = false
        isRecording = true
    }

    private fun stopRecording(save: Boolean) {
        if (save && elapsedSeconds <= 0) {
            Snackbar.make(binding.root, "请至少录制 1 秒后再保存", Snackbar.LENGTH_SHORT).show()
            return
        }
        val rec = recording
        if (rec == null) {
            Snackbar.make(binding.root, "录制未开始", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            shouldSaveCurrentRecording = save
            finishAfterFinalize = !save
            binding.pauseButton.isEnabled = false
            binding.stopButton.isEnabled = false
            binding.pauseButton.text = if (save) "保存中..." else "取消中..."
            rec.stop()
            recording = null
            handler.removeCallbacks(timerRunnable)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isRecording = false
            isPaused = false
            // Wait for VideoRecordEvent.Finalize so CameraX can return or clean the output Uri.
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording failed", e)
            Snackbar.make(binding.root, "停止录制失败：${e.message}", Snackbar.LENGTH_SHORT).show()
            recording = null
            requireActivity().finish()
        }
    }

    private fun buildFinalizeErrorMessage(event: VideoRecordEvent.Finalize): String {
        val baseMessage = when (event.error) {
            VideoRecordEvent.Finalize.ERROR_NONE -> "录制成功"
            VideoRecordEvent.Finalize.ERROR_UNKNOWN -> "录制失败，系统没有返回明确原因"
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> "录制已达到文件大小上限"
            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "存储空间不足，视频未完整保存"
            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "摄像头或录音源中断，录制被终止"
            VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS -> "视频保存路径无效，无法写入文件"
            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "视频编码失败，请重试一次"
            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR -> "录制器状态异常，请重新进入录制页"
            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "录制时长太短，还没生成可保存的视频"
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> "录制已达到时长上限"
            VideoRecordEvent.Finalize.ERROR_RECORDING_GARBAGE_COLLECTED -> "录制过程被系统中断，请重新开始"
            else -> "录制失败(error=${event.error})"
        }
        val detail = event.cause?.message?.takeIf { it.isNotBlank() }
        return detail?.let { "$baseMessage：$it" } ?: baseMessage
    }

    private fun cleanupFailedRecording(outputUri: android.net.Uri?) {
        if (outputUri == null || outputUri == android.net.Uri.EMPTY) return
        runCatching {
            if (outputUri.scheme == "file") {
                outputUri.path?.let { File(it).delete() }
            } else {
                requireContext().contentResolver.delete(outputUri, null, null)
            }
        }.onFailure { error ->
            Log.w(TAG, "cleanupFailedRecording failed for uri=$outputUri", error)
        }
    }

    private fun hasRootStorageAccess(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    private fun showRootStorageAccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("需要文件访问权限")
            .setMessage("视频将保存到手机根目录的 Talky 文件夹。请在系统设置里允许 Talky 访问所有文件。")
            .setPositiveButton("去设置") { _, _ -> openAllFilesAccessSettings() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val packageUri = Uri.parse("package:${requireContext().packageName}")
        val appIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri)
        val opened = startIntentSafely(appIntent)
        if (!opened) {
            startIntentSafely(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun startIntentSafely(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun updateTimerDisplay() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        binding.recIndicator.text = String.format(Locale.getDefault(), "REC %02d:%02d", minutes, seconds)
    }

    private fun resetRecordingUi() {
        isRecording = false
        isRecordingStarted = false
        isPaused = false
        recording = null
        elapsedSeconds = 0
        updateTimerDisplay()
        binding.pauseButton.isEnabled = true
        binding.pauseButton.text = "开始"
        binding.stopButton.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recording?.let { activeRecording ->
            shouldSaveCurrentRecording = false
            finishAfterFinalize = false
            runCatching { activeRecording.stop() }
            recording = null
        }
        isRecording = false
        isRecordingStarted = false
        isPaused = false
        handler.removeCallbacks(timerRunnable)
        cameraExecutor.shutdown()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        _binding = null
    }
}
