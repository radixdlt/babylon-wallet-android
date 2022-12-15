package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    preferencesManager: DataStoreManager
) : ViewModel() {

    private val _state: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasProfile = profileRepository.readProfileSnapshot() != null
            _state.update { state ->
                state.copy(
                    loading = false,
                    hasProfile = hasProfile,
                    showOnboarding = preferencesManager.showOnboarding()
                )
            }
        }
    }

    data class MainUiState(
        val loading: Boolean = true,
        val hasProfile: Boolean = false,
        val showOnboarding: Boolean = false
    )
}
