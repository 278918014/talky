package com.lixiangyang.talky.ui.recording

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lixiangyang.talky.data.repository.TalkyRepository
import kotlinx.coroutines.launch

class RecordingViewModel(
    private val repository: TalkyRepository
) : ViewModel() {
    companion object {
        fun factory(repository: TalkyRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RecordingViewModel(repository) as T
        }
    }

    private val _saveDone = MutableLiveData<Long?>()
    val saveDone: LiveData<Long?> = _saveDone

    fun savePractice(
        title: String,
        durationSeconds: Int,
        resolution: String,
        filePath: String,
        thumbnailPath: String
    ) {
        viewModelScope.launch {
            val id = repository.createPractice(title, durationSeconds, resolution, filePath, thumbnailPath)
            _saveDone.value = id
        }
    }

    fun onSaveHandled() {
        _saveDone.value = null
    }
}
