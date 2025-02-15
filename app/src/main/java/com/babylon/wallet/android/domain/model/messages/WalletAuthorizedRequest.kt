package com.babylon.wallet.android.domain.model.messages

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappToWalletInteractionResetRequestItem
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.WalletInteractionId

data class WalletAuthorizedRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val requestMetadata: RequestMetadata,
    val authRequestItem: AuthRequestItem,
    val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
    val ongoingAccountsRequestItem: AccountsRequestItem? = null,
    val oneTimePersonaDataRequestItem: PersonaDataRequestItem? = null,
    val ongoingPersonaDataRequestItem: PersonaDataRequestItem? = null,
    val resetRequestItem: DappToWalletInteractionResetRequestItem? = null,
    val proofOfOwnershipRequestItem: ProofOfOwnershipRequestItem? = null
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata) {

    override val isInternal: Boolean
        get() {
            return requestMetadata.isInternal || remoteEntityId.id.isEmpty()
        }

    val loginWithChallenge: Exactly32Bytes?
        get() {
            return when (authRequestItem) {
                is AuthRequestItem.LoginRequest.WithChallenge -> this.authRequestItem.challenge
                else -> null
            }
        }

    fun needSignatures(): Boolean {
        return authRequestItem is AuthRequestItem.LoginRequest.WithChallenge ||
            ongoingAccountsRequestItem?.challenge != null ||
            oneTimeAccountsRequestItem?.challenge != null
    }

    fun hasOngoingRequestItemsOnly(): Boolean {
        return isUsePersonaAuth() && hasNoOneTimeRequestItems() && hasNoResetRequestItem() &&
            (ongoingAccountsRequestItem != null || ongoingPersonaDataRequestItem != null)
    }

    private fun isUsePersonaAuth(): Boolean {
        return authRequestItem is AuthRequestItem.UsePersonaRequest
    }

    private fun hasNoOneTimeRequestItems(): Boolean {
        return oneTimePersonaDataRequestItem == null && oneTimeAccountsRequestItem == null
    }

    private fun hasNoResetRequestItem(): Boolean {
        return resetRequestItem?.personaData != true && resetRequestItem?.accounts != true
    }

    fun isOnlyLoginRequest(): Boolean {
        return ongoingAccountsRequestItem == null && ongoingPersonaDataRequestItem == null &&
            oneTimeAccountsRequestItem == null && oneTimePersonaDataRequestItem == null
    }

    fun isValidRequest(): Boolean {
        return ongoingAccountsRequestItem?.isValidRequestItem() != false &&
            oneTimeAccountsRequestItem?.isValidRequestItem() != false
    }

    sealed interface AuthRequestItem {
        sealed class LoginRequest : AuthRequestItem {
            data class WithChallenge(val challenge: Exactly32Bytes) : LoginRequest()
            data object WithoutChallenge : LoginRequest()
        }

        data class UsePersonaRequest(val identityAddress: IdentityAddress) : AuthRequestItem
    }

    data class ProofOfOwnershipRequestItem(
        val challenge: Exactly32Bytes,
        val accountAddresses: List<AccountAddress>? = null,
        val personaAddress: IdentityAddress? = null
    )
}
