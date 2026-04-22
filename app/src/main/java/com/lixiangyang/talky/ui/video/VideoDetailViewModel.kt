package com.lixiangyang.talky.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lixiangyang.talky.data.repository.TalkyRepository
import kotlinx.coroutines.launch

class VideoDetailViewModel(
    private val repository: TalkyRepository,
    private val practiceId: Long
) : ViewModel() {

    val practice = repository.observePractice(practiceId).asLiveData()

    fun deletePractice(onDone: () -> Unit) {
        viewModelScope.launch {
            // In a real implementation, delete from DB and file system
            onDone()
        }
    }

    companion object {
        fun factory(repository: TalkyRepository, practiceId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VideoDetailViewModel(repository, practiceId) as T
        }
    }
}
