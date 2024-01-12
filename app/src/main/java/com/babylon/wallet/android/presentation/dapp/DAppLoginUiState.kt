package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData

data class DAppLoginUiState(
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val failureDialog: FailureDialogState = FailureDialogState.Closed,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedOngoingPersonaData: PersonaData? = null,
    val selectedOneTimePersonaData: PersonaData? = null,
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null,
    val interactionState: InteractionState? = null,
    val isNoMnemonicErrorVisible: Boolean = false,
    val isAuthorizeRequest: Boolean = false,
    var authorizeRequest: MessageFromDataChannel.IncomingRequest.AuthorizedRequest? = null,
    var unAuthorizeRequest: MessageFromDataChannel.IncomingRequest.UnauthorizedRequest? = null,
    var requestId: String? = null,
    var authorizedDApp: Network.AuthorizedDapp? = null,
    var editedDApp: Network.AuthorizedDapp? = null
) : UiState {

    val remoteConnectionId: String
        get() = if (isAuthorizeRequest) {
            authorizeRequest?.remoteConnectorId.orEmpty()
        } else {
            unAuthorizeRequest?.remoteConnectorId.orEmpty()
        }
}
