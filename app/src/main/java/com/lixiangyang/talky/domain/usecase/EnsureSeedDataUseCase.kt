package com.lixiangyang.talky.domain.usecase

import com.lixiangyang.talky.data.repository.TalkyRepository

class EnsureSeedDataUseCase(
    private val repository: TalkyRepository
) {
    suspend operator fun invoke() {
        repository.ensureSeedData()
    }
}
