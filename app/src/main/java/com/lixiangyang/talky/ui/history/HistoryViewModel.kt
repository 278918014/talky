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
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(
    private val repository: TalkyRepository
) : ViewModel() {

    private val _selectedDate = MutableLiveData<Long?>()
    val selectedDate: LiveData<Long?> = _selectedDate

    private val dateFilterFlow = MutableStateFlow<Long?>(null)
    private val filterModeFlow = MutableStateFlow(HistoryFilter.RECENT)
    val selectedFilter: LiveData<HistoryFilter> = filterModeFlow.asLiveData()

    val practices = repository.observePractices().asLiveData()

    val availableDateOptions: LiveData<List<DateFilterOption>> =
        repository.observePractices()
            .map { practices ->
                val displayFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                practices
                    .groupBy { getDayStart(it.recordedAt) }
                    .map { (dayStart, items) ->
                        DateFilterOption(
                            dayStart = dayStart,
                            label = displayFormatter.format(Date(dayStart)),
                            count = items.size
                        )
                    }
                    .sortedByDescending { it.dayStart }
            }
            .asLiveData()

    val groupedPractices: LiveData<List<Pair<String, List<VideoPractice>>>> =
        combine(
            repository.observePractices(),
            dateFilterFlow,
            filterModeFlow
        ) { practices, dateFilter, filterMode ->
            val filtered = when (filterMode) {
                HistoryFilter.DATE -> {
                    val dayStart = getDayStart(dateFilter ?: System.currentTimeMillis())
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000
                    practices.filter { it.recordedAt >= dayStart && it.recordedAt < dayEnd }
                }
                HistoryFilter.WEEK -> {
                    val weekStart = getWeekStart(System.currentTimeMillis())
                    practices.filter { it.recordedAt >= weekStart }
                }
                HistoryFilter.MONTH -> {
                    val monthStart = getMonthStart(System.currentTimeMillis())
                    practices.filter { it.recordedAt >= monthStart }
                }
                HistoryFilter.LONGEST,
                HistoryFilter.RECENT -> practices
            }

            if (filterMode == HistoryFilter.LONGEST) {
                return@combine listOf("最长练习" to filtered.sortedByDescending { it.durationSeconds })
                    .filter { it.second.isNotEmpty() }
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
        filterModeFlow.value = HistoryFilter.DATE
    }

    fun clearDateFilter() {
        resetFilters()
    }

    fun resetFilters() {
        _selectedDate.value = null
        dateFilterFlow.value = null
        filterModeFlow.value = HistoryFilter.RECENT
    }

    fun setQuickFilter(filter: HistoryFilter) {
        _selectedDate.value = null
        dateFilterFlow.value = null
        filterModeFlow.value = filter
    }

    fun deletePractice(id: Long) {
        viewModelScope.launch {
            repository.deletePractice(id)
        }
    }

    fun deletePractices(ids: List<Long>) {
        viewModelScope.launch {
            repository.deletePractices(ids)
        }
    }

    private fun getDayStart(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getWeekStart(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getMonthStart(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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

    data class DateFilterOption(
        val dayStart: Long,
        val label: String,
        val count: Int
    )

    enum class HistoryFilter {
        RECENT,
        WEEK,
        MONTH,
        LONGEST,
        DATE
    }
}
