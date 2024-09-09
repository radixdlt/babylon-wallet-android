package com.babylon.wallet.android

import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.ProfileState
import rdx.works.core.sargon.isAdvancedLockEnabled
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockStateProvider @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    @ApplicationScope coroutineScope: CoroutineScope
) {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())

    val lockState = combine(
        getProfileUseCase.state,
        _state,
        getProfileUseCase.flow.map { it.isAdvancedLockEnabled }
    ) { profileState, lockedState, isAdvancedLockEnabled ->
        when {
            isAdvancedLockEnabled -> if (profileState is ProfileState.NotInitialised) {
                LockState.Unlocked
            } else {
                lockedState.lockState
            }

            else -> LockState.Unlocked
        }
    }.shareIn(scope = coroutineScope, started = SharingStarted.WhileSubscribed())

    suspend fun lockApp() {
        if (_state.value.isLockingPaused) return
        val isAppLockEnabled = getProfileUseCase.flow.firstOrNull()?.isAdvancedLockEnabled
        if (isAppLockEnabled == true) {
            _state.update { it.copy(lockState = LockState.Locked) }
        }
    }

    fun unlockApp() {
        _state.update { it.copy(lockState = LockState.Unlocked) }
    }

    fun pauseLocking() {
        _state.update { it.copy(isLockingPaused = true) }
    }

    fun resumeLocking() {
        _state.update { it.copy(isLockingPaused = false) }
    }

    private data class State(
        val isLockingPaused: Boolean = false,
        val lockState: LockState = LockState.Locked
    )

    sealed interface LockState {
        data object Locked : LockState
        data object Unlocked : LockState
    }
}
