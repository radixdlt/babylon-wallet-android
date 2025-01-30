package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ApplyShieldArgs
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ApplyShieldSharedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : StateViewModel<ApplyShieldSharedViewModel.State>() {

    private val args = ApplyShieldArgs(savedStateHandle)

    override fun initialState(): State = State(securityStructureId = args.securityStructureId)

    fun onAccountsSelected(addresses: List<AddressOfAccountOrPersona>) {
        _state.update { state -> state.copy(accountAddresses = addresses) }
    }

    fun onPersonasSelected(addresses: List<AddressOfAccountOrPersona>) {
        _state.update { state -> state.copy(personaAddresses = addresses) }
    }

    data class State(
        val securityStructureId: SecurityStructureId,
        val accountAddresses: List<AddressOfAccountOrPersona> = emptyList(),
        val personaAddresses: List<AddressOfAccountOrPersona> = emptyList()
    ) : UiState {

        val allAddresses: PersistentList<AddressOfAccountOrPersona> = (accountAddresses + personaAddresses).toPersistentList()
    }
}
