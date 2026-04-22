package com.lixiangyang.talky.core

import android.content.Context

object AppSettings {
    private const val PREFS_NAME = "talky_settings"
    private const val KEY_RESOLUTION = "recording_resolution"
    private const val DEFAULT_RESOLUTION_KEY = "hd"

    val resolutionOptions = listOf(
        ResolutionOption(
            key = "sd",
            label = "480P（省空间）",
            recordingLabel = "480P"
        ),
        ResolutionOption(
            key = "hd",
            label = "720P（推荐）",
            recordingLabel = "720P"
        ),
        ResolutionOption(
            key = "fhd",
            label = "1080P（高清）",
            recordingLabel = "1080P"
        )
    )

    fun getResolution(context: Context): ResolutionOption {
        val key = prefs(context).getString(KEY_RESOLUTION, DEFAULT_RESOLUTION_KEY)
        return resolutionOptions.firstOrNull { it.key == key } ?: defaultResolution()
    }

    fun getResolutionIndex(context: Context): Int {
        val selected = getResolution(context)
        return resolutionOptions.indexOfFirst { it.key == selected.key }.takeIf { it >= 0 } ?: 1
    }

    fun setResolution(context: Context, option: ResolutionOption) {
        prefs(context)
            .edit()
            .putString(KEY_RESOLUTION, option.key)
            .apply()
    }

    private fun defaultResolution(): ResolutionOption =
        resolutionOptions.first { it.key == DEFAULT_RESOLUTION_KEY }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class ResolutionOption(
        val key: String,
        val label: String,
        val recordingLabel: String
    )
}
