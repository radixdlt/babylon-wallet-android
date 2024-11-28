package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.EmailAddress
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataEntryPhoneNumber
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.WalletToDappInteractionAccountProof
import com.radixdlt.sargon.WalletToDappInteractionAuthProof
import com.radixdlt.sargon.WalletToDappInteractionPersonaDataRequestResponseItem
import com.radixdlt.sargon.WalletToDappInteractionPersonaProof
import com.radixdlt.sargon.WalletToDappInteractionProofOfOwnership
import com.radixdlt.sargon.WalletToDappInteractionProofOfOwnershipRequestResponseItem
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.publicKey
import com.radixdlt.sargon.extensions.signature

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

fun Map<ProfileEntity, SignatureWithPublicKey>.toWalletToDappInteractionProofOfOwnershipRequestResponseItem(
    challenge: Exactly32Bytes
): WalletToDappInteractionProofOfOwnershipRequestResponseItem {
    val entitiesWithProofs = this.map { (profileEntity, signatureWithPublicKey) ->
        when (profileEntity) {
            is ProfileEntity.PersonaEntity -> {
                WalletToDappInteractionProofOfOwnership.Persona(
                    v1 = WalletToDappInteractionPersonaProof(
                        identityAddress = profileEntity.identityAddress,
                        proof = signatureWithPublicKey.toWalletToDappInteractionAuthProof()
                    )
                )
            }

            is ProfileEntity.AccountEntity -> {
                WalletToDappInteractionProofOfOwnership.Account(
                    v1 = WalletToDappInteractionAccountProof(
                        accountAddress = profileEntity.accountAddress,
                        proof = signatureWithPublicKey.toWalletToDappInteractionAuthProof()
                    )
                )
            }
        }
    }

    return WalletToDappInteractionProofOfOwnershipRequestResponseItem(
        challenge = challenge,
        proofs = entitiesWithProofs
    )
}

fun SignatureWithPublicKey.toWalletToDappInteractionAuthProof() = WalletToDappInteractionAuthProof(
    publicKey = this.publicKey,
    curve = when (this.publicKey) {
        is PublicKey.Ed25519 -> Slip10Curve.CURVE25519
        is PublicKey.Secp256k1 -> Slip10Curve.SECP256K1
    },
    signature = this.signature
)
