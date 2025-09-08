package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ApplyShieldArgs
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ApplyShieldSharedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : StateViewModel<ApplyShieldSharedViewModel.State>() {

    private val args = ApplyShieldArgs(savedStateHandle)

    val mustSelectPersona
        get() = state.value.accountAddress == null

    override fun initialState(): State = State(securityStructureId = args.securityStructureId)

    fun onAccountSelected(address: AddressOfAccountOrPersona) {
        _state.update { state -> state.copy(accountAddress = address) }
    }

    fun onPersonaSelected(address: AddressOfAccountOrPersona) {
        _state.update { state -> state.copy(personaAddress = address) }
    }

    data class State(
        val securityStructureId: SecurityStructureId,
        val accountAddress: AddressOfAccountOrPersona? = null,
        val personaAddress: AddressOfAccountOrPersona? = null
    ) : UiState {

        val selectedAddress = accountAddress ?: personaAddress
    }
}
