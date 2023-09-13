package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.networkinfo.NetworkInfoRepository
import com.babylon.wallet.android.domain.common.asKotlinResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainnetAvailabilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val networkInfoRepository: NetworkInfoRepository
) {

    private val isAppBlocked = MutableStateFlow(false)
    val forceToMainnetMandatory = isAppBlocked.asStateFlow()

    suspend operator fun invoke() {
        if (isMainnetAvailable()) {
            isAppBlocked.update { true }
            profileRepository.clear()
        }
    }

    private suspend fun isMainnetAvailable() = networkInfoRepository.getMainnetAvailability().asKotlinResult().fold(
        onSuccess = { it },
        onFailure = { false }
    )
}
