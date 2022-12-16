package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    preferencesManager: DataStoreManager
) : ViewModel() {

    val state = preferencesManager.showOnboarding.map { showOnboarding ->
        MainUiState(
            loading = false,
            hasProfile = profileRepository.readProfileSnapshot() != null,
            showOnboarding = showOnboarding
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MainUiState()
    )

    data class MainUiState(
        val loading: Boolean = true,
        val hasProfile: Boolean = false,
        val showOnboarding: Boolean = false
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
