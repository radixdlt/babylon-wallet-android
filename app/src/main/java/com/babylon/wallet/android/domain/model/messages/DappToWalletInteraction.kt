package com.babylon.wallet.android.domain.model.messages

import android.os.Parcelable
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.WalletInteractionVersion
import com.radixdlt.sargon.newWalletInteractionVersionCurrent
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import rdx.works.core.sargon.PersonaDataField

sealed class DappToWalletInteraction(
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

    data class RequestMetadata(
        val version: WalletInteractionVersion,
        val networkId: NetworkId,
        val origin: String,
        val dAppDefinitionAddress: String,
        val isInternal: Boolean, // Indicates that the request is made from the wallet app itself.
        val blockUntilCompleted: Boolean = false,
    ) {

        companion object {
            fun internal(
                networkId: NetworkId,
                blockUntilCompleted: Boolean = false
            ) = RequestMetadata(
                version = newWalletInteractionVersionCurrent(),
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
    data class PersonaDataRequestItem(
        val isRequestingName: Boolean,
        val numberOfRequestedEmailAddresses: NumberOfValues? = null,
        val numberOfRequestedPhoneNumbers: NumberOfValues? = null,
        val isOngoing: Boolean
    ) : Parcelable {

        fun isValid(): Boolean {
            return isRequestingName || numberOfRequestedPhoneNumbers != null || numberOfRequestedEmailAddresses != null
        }

        fun toRequiredFields(): RequiredPersonaFields {
            return RequiredPersonaFields(
                mutableListOf<RequiredPersonaField>().also {
                    if (isRequestingName) {
                        it.add(
                            RequiredPersonaField(
                                PersonaDataField.Kind.Name,
                                NumberOfValues(
                                    1,
                                    DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
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
    }

    @Parcelize
    @Serializable
    data class NumberOfValues(
        val quantity: Int,
        val quantifier: Quantifier
    ) : Parcelable {

        enum class Quantifier {
            Exactly, AtLeast
        }

        fun exactly(): Boolean {
            return quantifier == Quantifier.Exactly
        }

        fun toRequestedNumberQuantifier(): RequestedNumberQuantifier = when (this.quantifier) {
            Quantifier.Exactly -> RequestedNumberQuantifier.EXACTLY
            Quantifier.AtLeast -> RequestedNumberQuantifier.AT_LEAST
        }
    }
}
