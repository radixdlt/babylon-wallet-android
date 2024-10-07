package com.babylon.wallet.android.domain.model.messages

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.WalletInteractionId

data class WalletUnauthorizedRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val requestMetadata: RequestMetadata,
    val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
    val oneTimePersonaDataRequestItem: PersonaDataRequestItem? = null,
    val proofOfOwnershipRequestItem: ProofOfOwnershipRequestItem? = null
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata) {

    data class ProofOfOwnershipRequestItem(
        val challenge: Exactly32Bytes,
        val accountAddresses: List<AccountAddress>? = null,
        val personaAddress: IdentityAddress? = null
    )

    fun isValidRequest(): Boolean {
        return oneTimeAccountsRequestItem?.isValidRequestItem() != false
    }
}
