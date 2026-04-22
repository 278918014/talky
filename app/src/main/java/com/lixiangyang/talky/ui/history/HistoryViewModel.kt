package com.lixiangyang.talky.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lixiangyang.talky.data.repository.TalkyRepository
import com.lixiangyang.talky.domain.model.VideoPractice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(
    private val repository: TalkyRepository
) : ViewModel() {

    private val _selectedDate = MutableLiveData<Long?>()
    val selectedDate: LiveData<Long?> = _selectedDate

    private val dateFilterFlow = MutableStateFlow<Long?>(null)

    val practices = repository.observePractices().asLiveData()

    val groupedPractices: LiveData<List<Pair<String, List<VideoPractice>>>> =
        combine(
            repository.observePractices(),
            dateFilterFlow
        ) { practices, dateFilter ->
            val filtered = if (dateFilter != null) {
                val dayStart = getDayStart(dateFilter)
                val dayEnd = dayStart + 24 * 60 * 60 * 1000
                practices.filter { it.recordedAt >= dayStart && it.recordedAt < dayEnd }
            } else {
                practices
            }

            // Group by date
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            filtered
                .sortedByDescending { it.recordedAt }
                .groupBy { formatter.format(Date(it.recordedAt)) }
                .map { (dateKey, items) ->
                    val displayKey = formatDisplayDate(dateKey)
                    displayKey to items.sortedByDescending { it.recordedAt }
                }
        }.asLiveData()

    fun setDateFilter(dateMillis: Long) {
        _selectedDate.value = dateMillis
        dateFilterFlow.value = dateMillis
    }

    fun clearDateFilter() {
        _selectedDate.value = null
        dateFilterFlow.value = null
    }

    private fun getDayStart(millis: Long): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = formatter.format(Date(millis))
        return formatter.parse(dateStr)?.time ?: millis
    }

    private fun formatDisplayDate(dateKey: String): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return try {
            val date = formatter.parse(dateKey)
            if (date != null) displayFormatter.format(date) else dateKey
        } catch (e: Exception) {
            dateKey
        }
    }

    companion object {
        fun factory(repository: TalkyRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(repository) as T
        }
    }
}
