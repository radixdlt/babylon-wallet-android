package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.PersonaDataName
import com.babylon.wallet.android.data.dapp.model.PersonaDataRequestResponseItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.utils.encodeUtf8
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.Network
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

fun PersonaData.PersonaDataField.isValid(): Boolean {
    return when (this) {
        else -> true
    }
}

fun PersonaData.PersonaDataField.isEmpty(): Boolean {
    return when (this) {
        is PersonaData.PersonaDataField.Email -> value.isEmpty()
        is PersonaData.PersonaDataField.Name -> fullName.isEmpty()
        is PersonaData.PersonaDataField.PhoneNumber -> value.isEmpty()
        else -> true
    }
}

val PersonaData.allNonEmptyFields: List<IdentifiedEntry<out PersonaData.PersonaDataField>>
    get() = allFields.filterNot { it.value.isEmpty() }

val PersonaData.PersonaDataField.Name.fullName: String
    get() {
        val fullName = if (variant == PersonaData.PersonaDataField.Name.Variant.Eastern) {
            listOf(family, given)
        } else {
            listOf(given, family)
        }.filter { it.isNotEmpty() }.joinToString(" ")
        return listOf(fullName, nickname).filter { it.isNotEmpty() }.joinToString("\n")
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

fun Network.Persona.getPersonaDataForFieldKinds(requiredPersonaFields: List<RequiredPersonaField>): PersonaData {
    return PersonaData(
        name = if (requiredPersonaFields.any { it.kind == PersonaData.PersonaDataField.Kind.Name }) personaData.name else null,
        dateOfBirth = if (requiredPersonaFields.any { it.kind == PersonaData.PersonaDataField.Kind.DateOfBirth }) {
            personaData.dateOfBirth
        } else {
            null
        },
        companyName = if (requiredPersonaFields.any { it.kind == PersonaData.PersonaDataField.Kind.CompanyName }) {
            personaData.companyName
        } else {
            null
        },
        emailAddresses = requiredPersonaFields.firstOrNull { it.kind == PersonaData.PersonaDataField.Kind.EmailAddress }?.let {
            val count = it.numberOfValues.quantity
            require(count <= personaData.emailAddresses.size)
            personaData.emailAddresses.take(count)
        }.orEmpty(),
        phoneNumbers = requiredPersonaFields.firstOrNull { it.kind == PersonaData.PersonaDataField.Kind.PhoneNumber }?.let {
            val count = it.numberOfValues.quantity
            require(count <= personaData.phoneNumbers.size)
            personaData.phoneNumbers.take(count)
        }.orEmpty(),
        urls = requiredPersonaFields.firstOrNull { it.kind == PersonaData.PersonaDataField.Kind.Url }?.let {
            val count = it.numberOfValues.quantity
            require(count <= personaData.urls.size)
            personaData.urls.take(count)
        }.orEmpty(),
        postalAddresses = requiredPersonaFields.firstOrNull { it.kind == PersonaData.PersonaDataField.Kind.PostalAddress }?.let {
            val count = it.numberOfValues.quantity
            require(count <= personaData.postalAddresses.size)
            personaData.postalAddresses.take(count)
        }.orEmpty(),
        creditCards = requiredPersonaFields.firstOrNull { it.kind == PersonaData.PersonaDataField.Kind.CreditCard }?.let {
            val count = it.numberOfValues.quantity
            require(count <= personaData.creditCards.size)
            personaData.creditCards.take(count)
        }.orEmpty()
    )
}
