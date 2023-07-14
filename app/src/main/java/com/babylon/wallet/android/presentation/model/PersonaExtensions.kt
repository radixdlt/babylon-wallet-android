package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.PersonaDataName
import com.babylon.wallet.android.data.dapp.model.PersonaDataRequestResponseItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.decodeUtf8
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.isValidEmail
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber

@StringRes
fun PersonaData.PersonaDataField.Kind.toDisplayResource(): Int {
    return when (this) {
        PersonaData.PersonaDataField.Kind.Name -> R.string.authorizedDapps_personaDetails_fullName
        PersonaData.PersonaDataField.Kind.EmailAddress -> R.string.authorizedDapps_personaDetails_emailAddress
        PersonaData.PersonaDataField.Kind.PhoneNumber -> R.string.authorizedDapps_personaDetails_phoneNumber
        else -> R.string.empty
    }
}

fun List<PersonaData.PersonaDataField.Kind>.encodeToString(): String {
    return joinToString(",") { it.name }.encodeUtf8()
}

fun String.decodePersonaDataKinds(): List<PersonaData.PersonaDataField.Kind> {
    return decodeUtf8().split(",").filter { it.isNotEmpty() }.map { PersonaData.PersonaDataField.Kind.valueOf(it) }
}

fun RequestedNumber.Quantifier.toQuantifierUsedInRequest():
        MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier {
    return when (this) {
        RequestedNumber.Quantifier.Exactly -> {
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
        }

        RequestedNumber.Quantifier.AtLeast -> {
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
        }
    }
}

fun PersonaData.PersonaDataField.sortOrderInt(): Int {
    return kind.ordinal
}

fun PersonaData.PersonaDataField.isValid(): Boolean {
    return when (this) {
        is PersonaData.PersonaDataField.Email -> value.trim().isValidEmail()
        is PersonaData.PersonaDataField.Name -> given.trim().isNotEmpty() && family.isNotEmpty()
        is PersonaData.PersonaDataField.PhoneNumber -> value.trim().isNotEmpty()
        else -> true
    }
}

fun PersonaData.PersonaDataField.Kind.empty(): PersonaData.PersonaDataField {
    return when (this) {
        PersonaData.PersonaDataField.Kind.Name -> PersonaData.PersonaDataField.Name(
            variant = PersonaData.PersonaDataField.Name.Variant.Western,
            given = "",
            family = "",
            nickname = ""
        )

        PersonaData.PersonaDataField.Kind.EmailAddress -> PersonaData.PersonaDataField.Email("")
        PersonaData.PersonaDataField.Kind.PhoneNumber -> PersonaData.PersonaDataField.PhoneNumber("")
        else -> throw RuntimeException("Field $this not supported")
    }
}

val PersonaData.PersonaDataField.Name.fullName: String
    get() = if (variant == PersonaData.PersonaDataField.Name.Variant.Eastern) {
        listOfNotNull(family, given).filter { it.isNotEmpty() }.joinToString(" ")
    } else {
        listOfNotNull(given, family).filter { it.isNotEmpty() }.joinToString(" ")
    }

fun List<PersonaData.PersonaDataField>.toPersonaData(): PersonaData {
    return PersonaData(
        name = filterIsInstance<PersonaData.PersonaDataField.Name>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        dateOfBirth = filterIsInstance<PersonaData.PersonaDataField.DateOfBirth>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        companyName = filterIsInstance<PersonaData.PersonaDataField.CompanyName>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        emailAddresses = filterIsInstance<PersonaData.PersonaDataField.Email>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        phoneNumbers = filterIsInstance<PersonaData.PersonaDataField.PhoneNumber>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        urls = filterIsInstance<PersonaData.PersonaDataField.Url>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        postalAddresses = filterIsInstance<PersonaData.PersonaDataField.PostalAddress>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        creditCards = filterIsInstance<PersonaData.PersonaDataField.CreditCard>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        }
    )
}

fun PersonaData.toPersonaDataRequestResponseItem(): PersonaDataRequestResponseItem {
    return PersonaDataRequestResponseItem(
        name = name?.value?.let { name -> PersonaDataName(name.variant.toVariantDTO(), name.family, name.given, name.nickname) },
        emailAddresses = emailAddresses.map { it.value.value },
        phoneNumbers = phoneNumbers.map { it.value.value }
    )
}

fun PersonaData.PersonaDataField.Name.Variant.toVariantDTO(): PersonaDataName.Variant {
    return when (this) {
        PersonaData.PersonaDataField.Name.Variant.Eastern -> PersonaDataName.Variant.Eastern
        PersonaData.PersonaDataField.Name.Variant.Western -> PersonaDataName.Variant.Western
    }
}
