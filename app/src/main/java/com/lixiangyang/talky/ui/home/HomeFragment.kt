package com.lixiangyang.talky.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lixiangyang.talky.R
import com.lixiangyang.talky.core.AuthManager
import com.lixiangyang.talky.databinding.FragmentHomeBinding
import com.lixiangyang.talky.ui.auth.LoginActivity
import com.lixiangyang.talky.ui.common.container
import com.lixiangyang.talky.ui.history.HistoryActivity
import com.lixiangyang.talky.ui.recording.RecordingActivity

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory(container().repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.dashboard.observe(viewLifecycleOwner) { summary ->
            // Hero card: "今天已练习 X 次"
            binding.todayCount.text = getString(R.string.dashboard_today_count, summary.todayCount)

            // Stats card: keep values short so they stay readable on small screens.
            binding.todayCountLabel.text = summary.todayCount.toString()
            binding.totalCount.text = summary.totalCount.toString()
        }

        binding.startRecordingButton.setOnClickListener {
            val context = requireContext()
            if (AuthManager(context).isLoggedIn) {
                startActivity(RecordingActivity.createIntent(context))
            } else {
                startActivity(LoginActivity.createIntent(context, openRecordingAfterLogin = true))
            }
        }

        binding.historyCard.setOnClickListener {
            startActivity(HistoryActivity.createIntent(requireContext()))
        }

        binding.todayStatsItem.setOnClickListener {
            startActivity(HistoryActivity.createIntent(requireContext()))
        }

        binding.totalStatsItem.setOnClickListener {
            startActivity(HistoryActivity.createIntent(requireContext()))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
