package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.ce.dapp.model.PersonaDataField
import rdx.works.profile.data.model.pernetwork.Network

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(
        val remoteClientId: String, // from which dapp comes the message
        val id: String, // the id of the request
        val metadata: RequestMetadata
    ) : MessageFromDataChannel {

        data class AuthorizedRequest(
            val dappId: String, // from which dapp comes the message
            val requestId: String,
            val requestMetadata: RequestMetadata,
            val authRequest: AuthRequest,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val ongoingAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaDataRequestItem: PersonaRequestItem? = null,
            val ongoingPersonaDataRequestItem: PersonaRequestItem? = null,
            val resetRequestItem: ResetRequestItem? = null
        ) : IncomingRequest(dappId, requestId, requestMetadata) {

            fun hasOngoingRequestItemsOnly(): Boolean {
                return isUsePersonaAuth() && hasNoOneTimeRequestItems() && hasNoResetRequestItem() &&
                        (ongoingAccountsRequestItem != null || ongoingPersonaDataRequestItem != null)
            }

            fun isInternalRequest(): Boolean {
                return dappId.isEmpty()
            }

            fun isUsePersonaAuth(): Boolean {
                return authRequest is AuthRequest.UsePersonaRequest
            }

            private fun hasNoOneTimeRequestItems(): Boolean {
                return oneTimePersonaDataRequestItem == null && oneTimeAccountsRequestItem == null
            }

            private fun hasNoResetRequestItem(): Boolean {
                return resetRequestItem?.personaData != true && resetRequestItem?.accounts != true
            }

            fun hasOnlyAuthItem(): Boolean {
                return ongoingAccountsRequestItem == null &&
                        ongoingPersonaDataRequestItem == null &&
                        oneTimeAccountsRequestItem == null &&
                        oneTimePersonaDataRequestItem == null
            }

            fun isValidRequest(): Boolean {
                return ongoingAccountsRequestItem?.isValidRequestItem() != false &&
                        oneTimeAccountsRequestItem?.isValidRequestItem() != false
            }

            sealed interface AuthRequest {
                data class LoginRequest(val challenge: String? = null) : AuthRequest
                data class UsePersonaRequest(val personaAddress: String) : AuthRequest
            }
        }

        data class UnauthorizedRequest(
            val dappId: String, // from which dapp comes the message
            val requestId: String,
            val requestMetadata: RequestMetadata,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaDataRequestItem: PersonaRequestItem? = null
        ) : IncomingRequest(dappId, requestId, requestMetadata) {
            fun isValidRequest(): Boolean {
                return oneTimeAccountsRequestItem?.isValidRequestItem() != false
            }
        }

        data class TransactionRequest(
            val dappId: String, // from which dapp comes the message
            val requestId: String,
            val transactionManifestData: TransactionManifestData,
            val requestMetadata: RequestMetadata
        ) : IncomingRequest(dappId, requestId, requestMetadata)

        data class RequestMetadata(
            val networkId: Int,
            val origin: String,
            val dAppDefinitionAddress: String
        )

        data class AccountsRequestItem(
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int,
            val quantifier: AccountNumberQuantifier
        ) {
            enum class AccountNumberQuantifier {
                Exactly, AtLeast;

                fun exactly(): Boolean {
                    return this == Exactly
                }
            }

            fun isValidRequestItem(): Boolean {
                return numberOfAccounts >= 0
            }
        }

        data class PersonaRequestItem(
            val fields: List<PersonaDataField>,
            val isOngoing: Boolean
        ) {
            fun isValid(): Boolean {
                return fields.isNotEmpty()
            }
        }

        data class ResetRequestItem(
            val accounts: Boolean,
            val personaData: Boolean
        )
    }

    sealed class LedgerResponse(val id: String) : MessageFromDataChannel {

        enum class LedgerDeviceModel {
            NanoS, NanoSPlus, NanoX;
        }

        data class GetDeviceInfoResponse(
            val interactionId: String,
            val model: LedgerDeviceModel,
            val deviceId: String
        ) : LedgerResponse(interactionId)

        data class DerivePublicKeyResponse(
            val interactionId: String,
            val publicKeyHex: String
        ) : LedgerResponse(interactionId)

        data class ImportOlympiaDeviceResponse(
            val interactionId: String,
            val model: LedgerDeviceModel,
            val deviceId: String,
            val derivedPublicKeys: List<DerivedPublicKey>
        ) : LedgerResponse(interactionId) {

            data class DerivedPublicKey(
                val publicKeyHex: String,
                val derivationPath: String
            )
        }

        data class SignTransactionResponse(
            val interactionId: String,
            val signature: String,
            val publicKeyHex: String
        ) : LedgerResponse(interactionId)

        data class SignChallengeResponse(
            val interactionId: String,
            val signature: String,
            val publicKeyHex: String
        ) : LedgerResponse(interactionId)

        data class LedgerErrorResponse(
            val interactionId: String,
            val code: Int,
            val message: String
        ) : LedgerResponse(interactionId)
    }


}

fun MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.toProfileShareAccountsQuantifier():
        Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier {
    return when (this) {
        MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly -> {
            Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly
        }
        MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast -> {
            Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast
        }
    }
}
