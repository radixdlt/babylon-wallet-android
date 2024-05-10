package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.CollectionOfEmailAddresses
import com.radixdlt.sargon.CollectionOfPhoneNumbers
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.PersonaDataEntryEmailAddress
import com.radixdlt.sargon.PersonaDataEntryID
import com.radixdlt.sargon.PersonaDataEntryName
import com.radixdlt.sargon.PersonaDataEntryPhoneNumber
import com.radixdlt.sargon.PersonaDataIdentifiedEmailAddress
import com.radixdlt.sargon.PersonaDataIdentifiedName
import com.radixdlt.sargon.PersonaDataIdentifiedPhoneNumber
import com.radixdlt.sargon.PersonaDataNameVariant
import rdx.works.core.sargon.IdentifiedEntry
import rdx.works.core.sargon.PersonaDataField

data class PersonaFieldWrapper(
    val entry: IdentifiedEntry<PersonaDataField>,
    val selected: Boolean = false,
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false,
    val id: PersonaDataEntryID = PersonaDataEntryID.randomUUID(),
) {
    fun isPhoneNumber(): Boolean {
        return entry.value.kind == PersonaDataField.Kind.PhoneNumber
    }
}

fun List<PersonaFieldWrapper>.toPersonaData(): PersonaData {
    val fields = map { it.entry }

    return PersonaData(
        name = fields.firstOrNull {
            it.value.kind == PersonaDataField.Kind.Name
        }?.let {
            val nameValue = it.value as PersonaDataField.Name
            PersonaDataIdentifiedName(
                id = it.uuid,
                value = PersonaDataEntryName(
                    variant = when (nameValue.variant) {
                        PersonaDataField.Name.Variant.Western -> PersonaDataNameVariant.WESTERN
                        PersonaDataField.Name.Variant.Eastern -> PersonaDataNameVariant.EASTERN
                    },
                    familyName = nameValue.family,
                    nickname = nameValue.nickname,
                    givenNames = nameValue.given
                )
            )
        },
        emailAddresses = fields.filter { it.value.kind == PersonaDataField.Kind.EmailAddress }.map {
            val emailField = it.value as PersonaDataField.Email
            PersonaDataIdentifiedEmailAddress(id = it.uuid, value = PersonaDataEntryEmailAddress(emailField.value))
        }.let { CollectionOfEmailAddresses(it) },
        phoneNumbers = fields.filter { it.value.kind == PersonaDataField.Kind.PhoneNumber }.map {
            val phoneValue = it.value as PersonaDataField.PhoneNumber
            PersonaDataIdentifiedPhoneNumber(id = it.uuid, value = PersonaDataEntryPhoneNumber(phoneValue.value))
        }.let { CollectionOfPhoneNumbers(it) }
    )
}
