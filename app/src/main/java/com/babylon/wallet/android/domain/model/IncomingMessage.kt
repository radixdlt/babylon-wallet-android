package com.babylon.wallet.android.domain.model

import android.os.Parcelable
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.DappToWalletInteractionResetRequestItem
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.WalletInteractionId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.sargon.PersonaDataField

sealed interface IncomingMessage {

    sealed interface RemoteEntityID {

        val value: String

        data class RadixMobileConnectRemoteSession(val id: String, val originVerificationUrl: String? = null) : RemoteEntityID {
            override val value: String
                get() = id

            val needOriginVerification: Boolean = originVerificationUrl != null
        }

        data class ConnectorId(val id: String) : RemoteEntityID {
            override val value: String
                get() = id
        }
    }

    sealed class IncomingRequest(
        open val remoteEntityId: RemoteEntityID, // from which remote source message came
        open val interactionId: WalletInteractionId, // the id of the request
        val metadata: RequestMetadata
    ) : IncomingMessage {

        open val isInternal: Boolean
            get() {
                return metadata.isInternal
            }

        val isMobileConnectRequest: Boolean
            get() = remoteEntityId is RemoteEntityID.RadixMobileConnectRemoteSession

        val needVerification: Boolean
            get() = (remoteEntityId as? RemoteEntityID.RadixMobileConnectRemoteSession)?.needOriginVerification == true

        val blockUntilComplete: Boolean
            get() {
                return metadata.blockUntilCompleted
            }

        data class AuthorizedRequest(
            override val remoteEntityId: RemoteEntityID, // from which remote CE comes the message
            override val interactionId: WalletInteractionId,
            val requestMetadata: RequestMetadata,
            val authRequest: AuthRequest,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val ongoingAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaDataRequestItem: PersonaRequestItem? = null,
            val ongoingPersonaDataRequestItem: PersonaRequestItem? = null,
            val resetRequestItem: DappToWalletInteractionResetRequestItem? = null
        ) : IncomingRequest(remoteEntityId, interactionId, requestMetadata) {

            override val isInternal: Boolean
                get() {
                    return requestMetadata.isInternal || remoteEntityId.value.isEmpty()
                }

            fun needSignatures(): Boolean {
                return authRequest is AuthRequest.LoginRequest.WithChallenge ||
                    ongoingAccountsRequestItem?.challenge != null ||
                    oneTimeAccountsRequestItem?.challenge != null
            }

            fun hasOngoingRequestItemsOnly(): Boolean {
                return isUsePersonaAuth() && hasNoOneTimeRequestItems() && hasNoResetRequestItem() &&
                    (ongoingAccountsRequestItem != null || ongoingPersonaDataRequestItem != null)
            }

            private fun isUsePersonaAuth(): Boolean {
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
                    data class WithChallenge(val challenge: Exactly32Bytes) : LoginRequest()
                    data object WithoutChallenge : LoginRequest()
                }

                data class UsePersonaRequest(val identityAddress: IdentityAddress) : AuthRequest
            }
        }

        data class UnauthorizedRequest(
            override val remoteEntityId: RemoteEntityID, // from which remote CE comes the message
            override val interactionId: WalletInteractionId,
            val requestMetadata: RequestMetadata,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaDataRequestItem: PersonaRequestItem? = null
        ) : IncomingRequest(remoteEntityId, interactionId, requestMetadata) {

            fun isValidRequest(): Boolean {
                return oneTimeAccountsRequestItem?.isValidRequestItem() != false
            }
        }

        data class TransactionRequest(
            override val remoteEntityId: RemoteEntityID, // from which remote CE comes the message
            override val interactionId: WalletInteractionId,
            val transactionManifestData: TransactionManifestData,
            val requestMetadata: RequestMetadata,
            val transactionType: TransactionType = TransactionType.Generic
        ) : IncomingRequest(remoteEntityId, interactionId, requestMetadata)

        data class RequestMetadata(
            val networkId: NetworkId,
            val origin: String,
            val dAppDefinitionAddress: String,
            val isInternal: Boolean, // Indicates that the request is made from the wallet app itself.
            val blockUntilCompleted: Boolean = false
        ) {

            companion object {
                fun internal(networkId: NetworkId, blockUntilCompleted: Boolean = false) = RequestMetadata(
                    networkId = networkId,
                    origin = "",
                    dAppDefinitionAddress = "",
                    isInternal = true,
                    blockUntilCompleted = blockUntilCompleted
                )
            }
        }

        data class AccountsRequestItem(
            val isOngoing: Boolean,
            val numberOfValues: NumberOfValues,
            val challenge: Exactly32Bytes?
        ) {

            fun isValidRequestItem(): Boolean {
                return numberOfValues.quantity >= 0
            }
        }

        @Serializable
        @Parcelize
        data class PersonaRequestItem(
            val isRequestingName: Boolean,
            val numberOfRequestedEmailAddresses: NumberOfValues? = null,
            val numberOfRequestedPhoneNumbers: NumberOfValues? = null,
            val isOngoing: Boolean
        ) : Parcelable {
            fun isValid(): Boolean {
                return isRequestingName || numberOfRequestedPhoneNumbers != null || numberOfRequestedEmailAddresses != null
            }
        }

        @Parcelize
        @Serializable
        data class NumberOfValues(
            val quantity: Int,
            val quantifier: Quantifier
        ) : Parcelable {

            fun exactly(): Boolean {
                return quantifier == Quantifier.Exactly
            }

            enum class Quantifier {
                Exactly, AtLeast
            }
        }
    }

    sealed class LedgerResponse(val id: String) : IncomingMessage {

        data class DerivedPublicKey(
            val curve: Curve,
            val publicKeyHex: String,
            val derivationPath: String
        ) {
            enum class Curve {
                Curve25519, Secp256k1
            }
        }

        data class DerivedAddress(
            val derivedKey: DerivedPublicKey,
            val address: String
        )

        enum class LedgerDeviceModel {
            NanoS, NanoSPlus, NanoX
        }

        data class SignatureOfSigner(
            val derivedPublicKey: DerivedPublicKey,
            val signature: String,
        )

        data class GetDeviceInfoResponse(
            val interactionId: String,
            val model: LedgerDeviceModel,
            val deviceId: Exactly32Bytes
        ) : LedgerResponse(interactionId) {

            val factorSourceId: FactorSourceId.Hash
                get() = FactorSourceId.Hash(
                    value = FactorSourceIdFromHash(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        body = deviceId
                    )
                )
        }

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

        data class DeriveAndDisplayAddressResponse(
            val interactionId: String,
            val derivedAddress: DerivedAddress
        ) : LedgerResponse(interactionId)

        data class LedgerErrorResponse(
            val interactionId: String,
            val code: LedgerErrorCode,
            val message: String
        ) : LedgerResponse(interactionId)
    }

    data object ParsingError : IncomingMessage

    data class Error(val exception: RadixWalletException) : IncomingMessage
}

fun IncomingMessage.IncomingRequest.NumberOfValues.toRequestedNumberQuantifier(): RequestedNumberQuantifier = when (quantifier) {
    IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly -> RequestedNumberQuantifier.EXACTLY
    IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.AtLeast -> RequestedNumberQuantifier.AT_LEAST
}

fun IncomingMessage.LedgerResponse.LedgerDeviceModel.toProfileLedgerDeviceModel(): LedgerHardwareWalletModel {
    return when (this) {
        IncomingMessage.LedgerResponse.LedgerDeviceModel.NanoS -> LedgerHardwareWalletModel.NANO_S
        IncomingMessage.LedgerResponse.LedgerDeviceModel.NanoSPlus -> LedgerHardwareWalletModel.NANO_S_PLUS
        IncomingMessage.LedgerResponse.LedgerDeviceModel.NanoX -> LedgerHardwareWalletModel.NANO_X
    }
}

fun IncomingMessage.IncomingRequest.PersonaRequestItem.toRequiredFields(): RequiredPersonaFields {
    return RequiredPersonaFields(
        mutableListOf<RequiredPersonaField>().also {
            if (isRequestingName) {
                it.add(
                    RequiredPersonaField(
                        PersonaDataField.Kind.Name,
                        IncomingMessage.IncomingRequest.NumberOfValues(
                            1,
                            IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
                        )
                    )
                )
            }
            if (numberOfRequestedEmailAddresses != null) {
                it.add(
                    RequiredPersonaField(
                        kind = PersonaDataField.Kind.EmailAddress,
                        numberOfValues = numberOfRequestedEmailAddresses
                    )
                )
            }
            if (numberOfRequestedPhoneNumbers != null) {
                it.add(
                    RequiredPersonaField(
                        kind = PersonaDataField.Kind.PhoneNumber,
                        numberOfValues = numberOfRequestedPhoneNumbers
                    )
                )
            }
        }
    )
}

sealed interface IncomingRequestResponse {
    data object SuccessRadixMobileConnect : IncomingRequestResponse
    data object SuccessCE : IncomingRequestResponse
}
