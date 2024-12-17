package com.babylon.wallet.android.domain.model.messages

import com.radixdlt.sargon.WalletInteractionId

data class WalletUnauthorizedRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val requestMetadata: RequestMetadata,
    val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
    val oneTimePersonaDataRequestItem: PersonaDataRequestItem? = null
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata) {

    fun isValidRequest(): Boolean {
        return oneTimeAccountsRequestItem?.isValidRequestItem() != false
    }
}
