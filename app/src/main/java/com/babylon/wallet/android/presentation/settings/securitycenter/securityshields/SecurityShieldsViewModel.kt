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
import com.radixdlt.sargon.extensions.isMain
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
            val mainSecurityShield = securityShields.find { it.metadata.isMain }
            val otherSecurityShields = securityShields.toMutableList().apply { remove(mainSecurityShield) }

            _state.update { state ->
                state.copy(
                    mainSecurityShield = mainSecurityShield?.toSecurityShieldCard(),
                    otherSecurityShields = otherSecurityShields.map { it.toSecurityShieldCard() }.toPersistentList(),
                    isLoading = false
                )
            }
        }.onFailure { error ->
            Timber.e("Failed to get security shields for display: $error")
        }
    }

    private fun resetSecurityShieldsList() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    isLoading = true,
                    mainSecurityShield = null,
                    otherSecurityShields = persistentListOf()
                )
            }
            getSecurityShields()
        }
    }

    private fun ShieldForDisplay.toSecurityShieldCard(): SecurityShieldCard {
        return SecurityShieldCard(
            shieldForDisplay = this,
            messages = persistentListOf()
        )
    }

    fun onChangeMainSecurityShieldClick() {
        _state.update { state -> state.copy(isMainSecurityShieldBottomSheetVisible = true) }
    }

    fun onSecurityShieldSelect(securityShieldCard: SecurityShieldCard) {
        _state.update { it.copy(selectedSecurityShieldId = securityShieldCard.id) }
    }

    fun onConfirmChangeMainSecurityShield() {
        viewModelScope.launch {
            _state.update { state -> state.copy(isChangingMainSecurityShieldInProgress = true) }
            _state.value.selectedSecurityShieldId?.let { id ->
                sargonOsManager.callSafely(defaultDispatcher) {
                    setMainSecurityStructure(shieldId = id)
                }.onFailure { error ->
                    Timber.e("Failed to set main security shield: $error")
                }
            }
            _state.update { state -> state.copy(isChangingMainSecurityShieldInProgress = false) }
            onDismissMainSecurityShieldBottomSheet()
            resetSecurityShieldsList()
        }
    }

    fun onDismissMainSecurityShieldBottomSheet() {
        _state.update { state ->
            state.copy(
                isMainSecurityShieldBottomSheetVisible = false,
                selectedSecurityShieldId = null
            )
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val mainSecurityShield: SecurityShieldCard? = null,
        val otherSecurityShields: PersistentList<SecurityShieldCard> = persistentListOf(),
        val isMainSecurityShieldBottomSheetVisible: Boolean = false,
        val selectedSecurityShieldId: SecurityStructureId? = null,
        val isChangingMainSecurityShieldInProgress: Boolean = false,
    ) : UiState {

        val isContinueButtonEnabled: Boolean
            get() = selectableOtherSecurityShieldIds.any { it.selected }

        val selectableOtherSecurityShieldIds: ImmutableList<Selectable<SecurityShieldCard>> = otherSecurityShields.map {
            Selectable(
                data = it,
                selected = selectedSecurityShieldId == it.id
            )
        }.toImmutableList()
    }
}
