package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaField
import com.babylon.wallet.android.utils.encodeUtf8
import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.RequestedNumberQuantifier
import rdx.works.core.sargon.IdentifiedEntry
import rdx.works.core.sargon.PersonaDataField

@StringRes
fun PersonaDataField.Kind.toDisplayResource(): Int {
    return when (this) {
        PersonaDataField.Kind.Name -> R.string.authorizedDapps_personaDetails_fullName
        PersonaDataField.Kind.EmailAddress -> R.string.authorizedDapps_personaDetails_emailAddress
        PersonaDataField.Kind.PhoneNumber -> R.string.authorizedDapps_personaDetails_phoneNumber
        else -> R.string.empty
    }
}

fun List<PersonaDataField.Kind>.encodeToString(): String {
    return joinToString(",") { it.name }.encodeUtf8()
}

fun RequestedNumberQuantifier.toQuantifierUsedInRequest(): DappToWalletInteraction.NumberOfValues.Quantifier = when (this) {
    RequestedNumberQuantifier.EXACTLY -> DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
    RequestedNumberQuantifier.AT_LEAST -> DappToWalletInteraction.NumberOfValues.Quantifier.AtLeast
}

@Suppress("UNUSED_EXPRESSION")
fun PersonaDataField.isValid(): Boolean {
    return when (this) {
        else -> true
    }
}

fun PersonaDataField.isEmpty(): Boolean {
    return when (this) {
        is PersonaDataField.Email -> value.isEmpty()
        is PersonaDataField.Name -> fullName.isEmpty()
        is PersonaDataField.PhoneNumber -> value.isEmpty()
        else -> true
    }
}

val PersonaDataField.Name.fullName: String
    get() {
        val fullName = if (variant == PersonaDataField.Name.Variant.Eastern) {
            listOf(family, given)
        } else {
            listOf(given, family)
        }.filter { it.isNotEmpty() }.joinToString(" ")
        val nickname = if (nickname.isNotEmpty()) "\"$nickname\"" else nickname
        return listOf(fullName, nickname).filter { it.isNotEmpty() }.joinToString("\n")
    }

fun PersonaDataField.sortOrderInt(): Int {
    return kind.ordinal
}

@Suppress("TooGenericExceptionThrown")
fun PersonaDataField.Kind.empty(): IdentifiedEntry<PersonaDataField> {
    val value = when (this) {
        PersonaDataField.Kind.Name -> PersonaDataField.Name(
            variant = PersonaDataField.Name.Variant.Western,
            given = "",
            family = "",
            nickname = ""
        )

        PersonaDataField.Kind.EmailAddress -> PersonaDataField.Email("")
        PersonaDataField.Kind.PhoneNumber -> PersonaDataField.PhoneNumber("")
        else -> throw RuntimeException("Field $this not supported")
    }
    return IdentifiedEntry.init(value)
}

fun Persona.getPersonaDataForFieldKinds(requiredPersonaFields: List<RequiredPersonaField>): PersonaData {
    return PersonaData(
        name = if (requiredPersonaFields.any { it.kind == PersonaDataField.Kind.Name }) personaData.name else null,
        emailAddresses = CollectionOfEmailAddresses(
            requiredPersonaFields.firstOrNull { it.kind == PersonaDataField.Kind.EmailAddress }?.let {
                val count = it.numberOfValues.quantity
                require(count <= personaData.emailAddresses.collection.size)
                personaData.emailAddresses.collection.take(count)
            }.orEmpty()
        ),
        phoneNumbers = CollectionOfPhoneNumbers(
            requiredPersonaFields.firstOrNull { it.kind == PersonaDataField.Kind.PhoneNumber }?.let {
                val count = it.numberOfValues.quantity
                require(count <= personaData.phoneNumbers.collection.size)
                personaData.phoneNumbers.collection.take(count)
            }.orEmpty()
        ),
    )
}
