package com.babylon.wallet.android

import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockStateProvider @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope coroutineScope: CoroutineScope
) {

    private val _lockState: MutableStateFlow<LockState> = MutableStateFlow(LockState.Locked)
    val state =
        combine(getProfileUseCase.state, _lockState, preferencesManager.isAppLockEnabled) { profileState, lockedState, isAppLockEnabled ->
            when {
                isAppLockEnabled -> if (profileState is ProfileState.NotInitialised) {
                    LockState.Unlocked
                } else {
                    lockedState
                }

                else -> LockState.Unlocked
            }
        }.shareIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed())

    suspend fun lockApp() {
        val isAppLockEnabled = preferencesManager.isAppLockEnabled.firstOrNull()
        if (isAppLockEnabled == true) {
            _lockState.update { LockState.Locked }
        }
    }

    fun unlockApp() {
        _lockState.update { LockState.Unlocked }
    }
}

sealed interface LockState {
    data object Locked : LockState
    data object Unlocked : LockState
}
