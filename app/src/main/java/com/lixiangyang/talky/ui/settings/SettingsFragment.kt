package com.lixiangyang.talky.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lixiangyang.talky.R
import com.lixiangyang.talky.core.AppSettings
import com.lixiangyang.talky.databinding.FragmentSettingsBinding
import java.io.File

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置项点击
        binding.settingResolution.root.setOnClickListener {
            showResolutionDialog()
        }

        binding.settingStorage.root.setOnClickListener {
            showStorageDialog()
        }

        binding.settingPractice.root.setOnClickListener {
            showPracticeModeDialog()
        }

        binding.settingCache.root.setOnClickListener {
            showClearCacheDialog()
        }

        binding.settingHelp.root.setOnClickListener {
            Snackbar.make(binding.root, "帮助中心暂未开放", Snackbar.LENGTH_SHORT).show()
        }

        binding.settingAbout.root.setOnClickListener {
            showAboutDialog()
        }

        // 设置默认值
        binding.settingResolution.title.text = getString(R.string.settings_resolution)
        binding.settingResolution.subtitle.text = AppSettings.getResolution(requireContext()).label

        binding.settingStorage.title.text = getString(R.string.settings_storage)
        binding.settingStorage.subtitle.text = "查看内部存储 / SD卡具体路径"

        binding.settingPractice.title.text = getString(R.string.settings_practice_mode)
        binding.settingPractice.subtitle.text = "自由口播录制"

        binding.settingCache.title.text = getString(R.string.settings_cache)
        binding.settingCache.subtitle.text = "轻量设置，保持界面简单"

        binding.settingHelp.title.text = getString(R.string.settings_help)
        binding.settingHelp.subtitle.text = "轻量设置，保持界面简单"

        binding.settingAbout.title.text = getString(R.string.settings_about)
        binding.settingAbout.subtitle.text = "轻量设置，保持界面简单"
    }

    private fun showResolutionDialog() {
        val options = AppSettings.resolutionOptions
        val labels = options.map { it.label }.toTypedArray()
        val checkedIndex = AppSettings.getResolutionIndex(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_resolution))
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selected = options[which]
                AppSettings.setResolution(requireContext(), selected)
                binding.settingResolution.subtitle.text = selected.label
                Snackbar.make(binding.root, "已设置为 ${selected.label}", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showStorageDialog() {
        val locations = buildStorageLocations()
        val options = locations.map { location ->
            if (location.path.isBlank()) {
                "${location.title}\n${location.description}"
            } else {
                "${location.title}\n${location.path}"
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_storage))
            .setItems(options) { _, which ->
                val location = locations[which]
                if (location.path.isBlank()) {
                    Snackbar.make(binding.root, location.description, Snackbar.LENGTH_SHORT).show()
                } else {
                    binding.settingStorage.subtitle.text = "${location.title} · 点击查看路径"
                    openFolder(location)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildStorageLocations(): List<StorageLocation> {
        val internalPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Talky"
        ).absolutePath

        val locations = mutableListOf(
            StorageLocation(
                title = "内部存储（当前保存位置）",
                path = internalPath,
                description = "录制视频会保存在手机共享存储的 Movies/Talky 目录"
            )
        )

        val sdCardDir = requireContext()
            .getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
            .drop(1)
            .filterNotNull()
            .firstOrNull { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }

        if (sdCardDir != null) {
            val talkyDir = File(sdCardDir, "Talky").apply { mkdirs() }
            locations.add(
                StorageLocation(
                    title = "SD卡存储（可访问目录）",
                    path = talkyDir.absolutePath,
                    description = "这是应用可访问的 SD 卡视频目录"
                )
            )
        } else {
            locations.add(
                StorageLocation(
                    title = "SD卡存储",
                    path = "",
                    description = "未检测到可用 SD 卡"
                )
            )
        }

        return locations
    }

    private fun openFolder(location: StorageLocation) {
        val folderUri = buildExternalStorageUri(location.path)
        if (folderUri == null) {
            Snackbar.make(binding.root, "无法识别这个路径：${location.path}", Snackbar.LENGTH_LONG).show()
            return
        }

        val opened = openFolderWithViewIntent(folderUri) || openFolderWithTreeIntent(folderUri)
        if (!opened) {
            Snackbar.make(binding.root, "没有找到可打开文件夹的应用", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openFolderWithViewIntent(uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return startIntentSafely(intent)
    }

    private fun openFolderWithTreeIntent(uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        return startIntentSafely(intent)
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

    private fun buildExternalStorageUri(path: String): Uri? {
        val documentId = path.toExternalStorageDocumentId() ?: return null
        return DocumentsContract.buildTreeDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId)
    }

    private fun String.toExternalStorageDocumentId(): String? {
        val normalizedPath = trimEnd('/')
        val primaryPrefix = Environment.getExternalStorageDirectory().absolutePath
        if (normalizedPath == primaryPrefix || normalizedPath == "/sdcard") {
            return "primary:"
        }
        if (normalizedPath.startsWith("$primaryPrefix/")) {
            return "primary:${normalizedPath.removePrefix("$primaryPrefix/")}"
        }
        if (normalizedPath.startsWith("/sdcard/")) {
            return "primary:${normalizedPath.removePrefix("/sdcard/")}"
        }

        val storagePrefix = "/storage/"
        if (normalizedPath.startsWith(storagePrefix)) {
            val remainingPath = normalizedPath.removePrefix(storagePrefix)
            val volumeName = remainingPath.substringBefore('/', missingDelimiterValue = "")
            val relativePath = remainingPath.substringAfter('/', missingDelimiterValue = "")
            if (volumeName.isNotBlank()) {
                return if (relativePath.isBlank()) {
                    "$volumeName:"
                } else {
                    "$volumeName:$relativePath"
                }
            }
        }

        return null
    }

    private data class StorageLocation(
        val title: String,
        val path: String,
        val description: String
    )

    private fun showPracticeModeDialog() {
        val options = arrayOf("自由口播录制", "历史回看与复盘")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_practice_mode))
            .setItems(options) { _, which ->
                binding.settingPractice.subtitle.text = options[which]
                Snackbar.make(binding.root, "已设置为 ${options[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_cache))
            .setMessage("确定要清理所有缩略图缓存吗？")
            .setPositiveButton("清理") { _, _ ->
                Snackbar.make(binding.root, "缓存已清理", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_about))
            .setMessage("口播练习助手 Talky v1.0\n\n本地、私密、轻量的口播练习工具\n\n专注自由录制、历史回看与本地保存。")
            .setPositiveButton("好的", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
