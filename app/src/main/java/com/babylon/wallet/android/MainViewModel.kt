package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecase.ShowOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    showOnboardingUseCase: ShowOnboardingUseCase
) : ViewModel() {

    val state = showOnboardingUseCase.showOnboarding.map { showOnboarding ->
        MainUiState(
            loading = false,
            profileExists = profileRepository.readProfileSnapshot() != null,
            showOnboarding = showOnboarding
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MainUiState()
    )

    data class MainUiState(
        val loading: Boolean = true,
        val profileExists: Boolean = false,
        val showOnboarding: Boolean = false
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
