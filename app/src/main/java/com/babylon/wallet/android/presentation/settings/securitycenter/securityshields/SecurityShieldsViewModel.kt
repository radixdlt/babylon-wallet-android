package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.ShieldForDisplay
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityShieldsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SecurityShieldsViewModel.State>() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            getSecurityShields()
        }
    }

    private suspend fun getSecurityShields() {
        sargonOsManager.callSafely(defaultDispatcher) {
            val securityShields = getShieldsForDisplay()

            _state.update { state ->
                state.copy(
                    shields = securityShields.map { it.toSecurityShieldCard() }.toPersistentList(),
                    isLoading = false
                )
            }
        }.onFailure { error ->
            Timber.e("Failed to get security shields for display: $error")
        }
    }

    private fun ShieldForDisplay.toSecurityShieldCard(): SecurityShieldCard {
        return SecurityShieldCard(
            shieldForDisplay = this,
            messages = persistentListOf()
        )
    }

    data class State(
        val isLoading: Boolean = true,
        val shields: PersistentList<SecurityShieldCard> = persistentListOf(),
        val selectedSecurityShieldId: SecurityStructureId? = null
    ) : UiState {

        val isContinueButtonEnabled: Boolean
            get() = selectableSecurityShieldIds.any { it.selected }

        val selectableSecurityShieldIds: ImmutableList<Selectable<SecurityShieldCard>> = shields.map {
            Selectable(
                data = it,
                selected = selectedSecurityShieldId == it.id
            )
        }.toImmutableList()
    }
}
