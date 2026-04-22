package com.lixiangyang.talky.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lixiangyang.talky.R
import com.lixiangyang.talky.databinding.FragmentSettingsBinding

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
        binding.settingResolution.subtitle.text = "720P（推荐）"

        binding.settingStorage.title.text = getString(R.string.settings_storage)
        binding.settingStorage.subtitle.text = "内部存储"

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
        val options = arrayOf("480P（省空间）", "720P（推荐）", "1080P（高清）")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_resolution))
            .setItems(options) { _, which ->
                binding.settingResolution.subtitle.text = options[which]
                Snackbar.make(binding.root, "已设置为 ${options[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showStorageDialog() {
        val options = arrayOf("内部存储", "SD卡存储")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_storage))
            .setItems(options) { _, which ->
                binding.settingStorage.subtitle.text = options[which]
                Snackbar.make(binding.root, "已设置为 ${options[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

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
}
