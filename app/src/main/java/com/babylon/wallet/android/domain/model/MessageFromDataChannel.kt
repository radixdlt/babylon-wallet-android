package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.dapp.model.PersonaData
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.Network

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(
        val remoteClientId: String, // from which dapp comes the message
        val id: String, // the id of the request
        val metadata: RequestMetadata
    ) : MessageFromDataChannel {

        val isInternal: Boolean
            get() {
                return metadata.isInternal
            }

        data class AuthorizedRequest(
            val dappId: String, // from which dapp comes the message
            val interactionId: String,
            val requestMetadata: RequestMetadata,
            val authRequest: AuthRequest,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val ongoingAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaDataRequestItem: PersonaRequestItem? = null,
            val ongoingPersonaDataRequestItem: PersonaRequestItem? = null,
            val resetRequestItem: ResetRequestItem? = null
        ) : IncomingRequest(dappId, interactionId, requestMetadata) {

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
                return ongoingAccountsRequestItem == null && ongoingPersonaDataRequestItem == null &&
                    oneTimeAccountsRequestItem == null && oneTimePersonaDataRequestItem == null
            }

            fun isValidRequest(): Boolean {
                return ongoingAccountsRequestItem?.isValidRequestItem() != false &&
                    oneTimeAccountsRequestItem?.isValidRequestItem() != false
            }

            sealed interface AuthRequest {
                sealed class LoginRequest : AuthRequest {
                    data class WithChallenge(val challenge: String) : LoginRequest()
                    object WithoutChallenge : LoginRequest()
                }

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
            val requestMetadata: RequestMetadata,
        ) : IncomingRequest(dappId, requestId, requestMetadata)

        data class RequestMetadata(
            val networkId: Int,
            val origin: String,
            val dAppDefinitionAddress: String,
            val isInternal: Boolean // Indicates that the request is made from the wallet app itself.
        ) {

            companion object {
                fun internal(networkId: Int) = RequestMetadata(
                    networkId = networkId,
                    origin = "",
                    dAppDefinitionAddress = "",
                    isInternal = true
                )
            }
        }

        data class AccountsRequestItem(
            val isOngoing: Boolean,
            val numberOfAccounts: Int,
            val quantifier: AccountNumberQuantifier,
            val challenge: String?
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
            val fields: List<PersonaData.PersonaDataField>,
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

        data class DerivedPublicKey(
            val curve: Curve,
            val publicKeyHex: String,
            val derivationPath: String
        ) {
            enum class Curve {
                Curve25519, Secp256k1
            }
        }

        enum class LedgerDeviceModel {
            NanoS, NanoSPlus, NanoX;
        }

        data class SignatureOfSigner(
            val derivedPublicKey: DerivedPublicKey,
            val signature: String,
        )

        data class GetDeviceInfoResponse(
            val interactionId: String,
            val model: LedgerDeviceModel,
            val deviceId: String
        ) : LedgerResponse(interactionId)

        data class DerivePublicKeyResponse(
            val interactionId: String,
            val publicKeysHex: List<DerivedPublicKey>
        ) : LedgerResponse(interactionId)

        data class SignTransactionResponse(
            val interactionId: String,
            val signatures: List<SignatureOfSigner>
        ) : LedgerResponse(interactionId)

        data class SignChallengeResponse(
            val interactionId: String,
            val signatures: List<SignatureOfSigner>
        ) : LedgerResponse(interactionId)

        data class LedgerErrorResponse(
            val interactionId: String,
            val code: Int,
            val message: String
        ) : LedgerResponse(interactionId)
    }

    object ParsingError : MessageFromDataChannel

    object Error : MessageFromDataChannel
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

fun MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.toProfileLedgerDeviceModel():
    LedgerHardwareWalletFactorSource.DeviceModel {
    return when (this) {
        MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS -> LedgerHardwareWalletFactorSource.DeviceModel.NANO_S
        MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoSPlus -> LedgerHardwareWalletFactorSource.DeviceModel.NANO_S_PLUS
        MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoX -> LedgerHardwareWalletFactorSource.DeviceModel.NANO_X
    }
}
