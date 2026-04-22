package com.lixiangyang.talky.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lixiangyang.talky.data.repository.TalkyRepository
import com.lixiangyang.talky.domain.usecase.EnsureSeedDataUseCase
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TalkyRepository
) : ViewModel() {
    val dashboard = repository.observeDashboardSummary().asLiveData()

    init {
        viewModelScope.launch {
            EnsureSeedDataUseCase(repository).invoke()
        }
    }

    companion object {
        fun factory(repository: TalkyRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
        }
    }
}
