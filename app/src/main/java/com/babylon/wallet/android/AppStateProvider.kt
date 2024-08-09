package com.babylon.wallet.android

import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateProvider @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope coroutineScope: CoroutineScope
) {

    private val _lockState: MutableStateFlow<LockState> = MutableStateFlow(LockState.Locked)
    val state = combine(getProfileUseCase.state, _lockState) { profileState, lockedState ->
        val isAppLockEnabled = preferencesManager.isAppLockEnabled.firstOrNull()
        when {
            isAppLockEnabled == true -> if (profileState is ProfileState.NotInitialised) {
                LockState.Unlocked
            } else {
                lockedState
            }

            else -> LockState.Unlocked
        }
    }.shareIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed())

    val isDevBannerVisible = combine(getProfileUseCase.state, _lockState) { profileState, lockState ->
        when (profileState) {
            is ProfileState.Restored -> {
                lockState != LockState.Locked && !profileState.isCurrentNetworkMainnet()
            }

            else -> false
        }
    }

    suspend fun lockApp() {
        preferencesManager.isAppLockEnabled.firstOrNull()?.let { enabled ->
            if (enabled) {
                _lockState.tryEmit(LockState.Locked)
            }
        }
    }

    fun unlockApp() {
        _lockState.tryEmit(LockState.Unlocked)
    }
}

sealed interface LockState {
    data object Locked : LockState
    data object Unlocked : LockState
}
