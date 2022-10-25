package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppAccountUiState
import com.babylon.wallet.android.data.dapp.PersonaEntityUiState

sealed interface DAppState {
    data class ConnectionRequest(val labels: List<String>) : DAppState
    data class SelectPersona(val personas: List<PersonaEntityUiState>, val dismiss: Boolean) : DAppState
    data class SelectAccount(val accounts: List<DAppAccountUiState>, val dismiss: Boolean) : DAppState
}
