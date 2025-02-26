package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.EmailAddress
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatureOfOwner
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataEntryPhoneNumber
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.WalletToDappInteractionPersonaDataRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionProofOfOwnership
import com.radixdlt.sargon.WalletToDappInteractionProofOfOwnershipRequestResponseItem
import com.radixdlt.sargon.extensions.init

fun PersonaData.toWalletToDappInteractionPersonaDataRequestResponseItem(): WalletToDappInteractionPersonaDataRequestResponseItem {
    return WalletToDappInteractionPersonaDataRequestResponseItem(
        name = name?.value?.let { name ->
            PersonaDataEntryName(
                variant = when (name.variant) {
                    PersonaDataNameVariant.EASTERN -> PersonaDataNameVariant.EASTERN
                    PersonaDataNameVariant.WESTERN -> PersonaDataNameVariant.WESTERN
                },
                familyName = name.familyName,
                givenNames = name.givenNames,
                nickname = name.nickname
            )
        },
        emailAddresses = emailAddresses.collection.map {
            EmailAddress(it.value.email)
        },
        phoneNumbers = phoneNumbers.collection.map {
            PersonaDataEntryPhoneNumber(it.value.number)
        }
    )
}

fun Map<AddressOfAccountOrPersona, SignatureWithPublicKey>.toWalletToDappInteractionProofOfOwnershipRequestResponseItem(
    challenge: Exactly32Bytes
): WalletToDappInteractionProofOfOwnershipRequestResponseItem {
    val proofs = map { (entityAddress, signatureWithPublicKey) ->
        IntentSignatureOfOwner(
            owner = entityAddress,
            intentSignature = IntentSignature.init(signatureWithPublicKey)
        )
    }.map { intentSignatureOfOwner ->
        WalletToDappInteractionProofOfOwnership.init(intentSignatureOfOwner)
    }

    return WalletToDappInteractionProofOfOwnershipRequestResponseItem(
        challenge = challenge,
        proofs = proofs
    )
}
