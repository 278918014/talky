package com.lixiangyang.talky.ui.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.selectedDate.value ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.setDateFilter(selected.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
