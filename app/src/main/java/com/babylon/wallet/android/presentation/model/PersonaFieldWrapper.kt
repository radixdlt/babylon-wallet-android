package com.babylon.wallet.android.presentation.model

import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

data class PersonaFieldWrapper(
    val value: PersonaData.PersonaDataField,
    val selected: Boolean = false,
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false,
    val id: PersonaDataEntryID = UUIDGenerator.uuid().toString(),
) {
    fun isPhoneNumber(): Boolean {
        return value.kind == PersonaData.PersonaDataField.Kind.PhoneNumber
    }
}

fun List<PersonaFieldWrapper>.toPersonaData(): PersonaData {
    val fields = mapNotNull { it.value }
    return PersonaData(
        name = fields.filterIsInstance<PersonaData.PersonaDataField.Name>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        dateOfBirth = fields.filterIsInstance<PersonaData.PersonaDataField.DateOfBirth>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        companyName = fields.filterIsInstance<PersonaData.PersonaDataField.CompanyName>().firstOrNull()?.let { IdentifiedEntry.init(it) },
        emailAddresses = fields.filterIsInstance<PersonaData.PersonaDataField.Email>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        phoneNumbers = fields.filterIsInstance<PersonaData.PersonaDataField.PhoneNumber>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        urls = fields.filterIsInstance<PersonaData.PersonaDataField.Url>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        postalAddresses = fields.filterIsInstance<PersonaData.PersonaDataField.PostalAddress>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        },
        creditCards = fields.filterIsInstance<PersonaData.PersonaDataField.CreditCard>().let { field ->
            field.map { IdentifiedEntry.init(it) }
        }
    )
}
