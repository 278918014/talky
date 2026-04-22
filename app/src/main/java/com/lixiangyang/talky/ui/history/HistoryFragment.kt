package com.lixiangyang.talky.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lixiangyang.talky.R
import com.lixiangyang.talky.databinding.FragmentHistoryBinding
import com.lixiangyang.talky.ui.common.container
import com.lixiangyang.talky.ui.video.VideoPlayerActivity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {
    companion object {
        private const val ARG_AUTOPLAY_PRACTICE_ID = "extra_autoplay_practice_id"
    }

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModel.factory(container().repository)
    }

    private val adapter = HistoryAdapter { item ->
        startActivity(VideoPlayerActivity.createIntent(requireContext(), item.id))
    }

    private val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    private var availableDateOptions: List<HistoryViewModel.DateFilterOption> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { requireActivity().finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Date filter click
        binding.dateFilterCard.setOnClickListener {
            showDatePicker()
        }

        binding.clearFilter.setOnClickListener {
            viewModel.clearDateFilter()
            binding.dateFilterText.text = "最近录制"
            binding.clearFilter.visibility = View.GONE
        }

        viewModel.groupedPractices.observe(viewLifecycleOwner) { grouped ->
            if (grouped.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitGrouped(grouped)
            }
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                binding.dateFilterText.text = dateFormatter.format(Date(date))
                binding.clearFilter.visibility = View.VISIBLE
            } else {
                binding.dateFilterText.text = "最近录制"
                binding.clearFilter.visibility = View.GONE
            }
        }

        viewModel.availableDateOptions.observe(viewLifecycleOwner) { options ->
            availableDateOptions = options
        }
    }

    private fun showDatePicker() {
        val availableDates = availableDateOptions.map { it.dayStart }.toSet()
        val initialMonth = Calendar.getInstance().apply {
            timeInMillis = viewModel.selectedDate.value
                ?: availableDateOptions.firstOrNull()?.dayStart
                    ?: System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (availableDateOptions.isEmpty()) {
            Toast.makeText(requireContext(), "还没有可筛选的录制日期", Toast.LENGTH_SHORT).show()
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val header = LinearLayout(requireContext()).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val prevButton = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            background = null
            contentDescription = "上个月"
        }
        val nextButton = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_media_next)
            background = null
            contentDescription = "下个月"
        }
        val title = TextView(requireContext()).apply {
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.talky_text, null))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val calendarGrid = GridLayout(requireContext()).apply {
            columnCount = 7
            useDefaultMargins = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        header.addView(prevButton, LinearLayout.LayoutParams(dp(44), dp(44)))
        header.addView(title)
        header.addView(nextButton, LinearLayout.LayoutParams(dp(44), dp(44)))
        content.addView(header)
        content.addView(calendarGrid)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择日期")
            .setView(content)
            .setNegativeButton("取消", null)
            .create()

        fun renderMonth() {
            title.text = monthFormatter.format(initialMonth.time)
            calendarGrid.removeAllViews()
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekLabel ->
                calendarGrid.addView(createWeekLabel(weekLabel))
            }

            val monthCursor = initialMonth.clone() as Calendar
            val leadingBlanks = monthCursor.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            repeat(leadingBlanks) {
                calendarGrid.addView(View(requireContext()), dayLayoutParams())
            }

            val daysInMonth = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..daysInMonth) {
                val dayCalendar = initialMonth.clone() as Calendar
                dayCalendar.set(Calendar.DAY_OF_MONTH, day)
                val dayStart = dayCalendar.timeInMillis
                calendarGrid.addView(createDayView(day, dayStart, availableDates, dialog))
            }
        }

        prevButton.setOnClickListener {
            initialMonth.add(Calendar.MONTH, -1)
            renderMonth()
        }
        nextButton.setOnClickListener {
            initialMonth.add(Calendar.MONTH, 1)
            renderMonth()
        }

        renderMonth()
        dialog.show()
    }

    private fun createWeekLabel(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.talky_subtext, null))
            textSize = 12f
            layoutParams = dayLayoutParams()
        }
    }

    private fun createDayView(
        day: Int,
        dayStart: Long,
        availableDates: Set<Long>,
        dialog: androidx.appcompat.app.AlertDialog
    ): TextView {
        val isAvailable = dayStart in availableDates
        val isSelected = viewModel.selectedDate.value == dayStart
        return TextView(requireContext()).apply {
            text = day.toString()
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            layoutParams = dayLayoutParams()
            background = when {
                isSelected -> resources.getDrawable(R.drawable.bg_calendar_day_selected, null)
                isAvailable -> resources.getDrawable(R.drawable.bg_calendar_day_available, null)
                else -> resources.getDrawable(R.drawable.bg_calendar_day_empty, null)
            }
            setTextColor(
                resources.getColor(
                    when {
                        isSelected -> android.R.color.white
                        isAvailable -> R.color.talky_primary_deep
                        else -> R.color.talky_subtext
                    },
                    null
                )
            )
            alpha = if (isAvailable) 1f else 0.55f
            setOnClickListener {
                if (isAvailable) {
                    viewModel.setDateFilter(dayStart)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "这天未录制视频", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dayLayoutParams(): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = dp(40)
            height = dp(40)
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
