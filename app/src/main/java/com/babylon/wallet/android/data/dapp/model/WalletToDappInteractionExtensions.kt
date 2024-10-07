package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.EmailAddress
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataEntryPhoneNumber
import com.radixdlt.sargon.PersonaDataNameVariant
import com.radixdlt.sargon.WalletToDappInteractionPersonaDataRequestResponseItem

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
