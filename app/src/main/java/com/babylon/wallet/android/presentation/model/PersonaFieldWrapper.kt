package com.babylon.wallet.android.presentation.model

import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.IdentifiedEntry
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

data class PersonaFieldWrapper(
    val entry: IdentifiedEntry<PersonaData.PersonaDataField>,
    val selected: Boolean = false,
    val valid: Boolean? = null,
    val required: Boolean = false,
    val wasEdited: Boolean = false,
    val shouldDisplayValidationError: Boolean = false,
    val id: PersonaDataEntryID = UUIDGenerator.uuid().toString(),
) {
    fun isPhoneNumber(): Boolean {
        return entry.value.kind == PersonaData.PersonaDataField.Kind.PhoneNumber
    }
}

fun List<PersonaFieldWrapper>.toPersonaData(): PersonaData {
    val fields = map { it.entry }
    return PersonaData(
        name = fields.firstOrNull { it.value.kind == PersonaData.PersonaDataField.Kind.Name }?.let {
            val nameValue = it.value as PersonaData.PersonaDataField.Name
            IdentifiedEntry.init(nameValue, it.id)
        },
        dateOfBirth = fields.firstOrNull { it.value.kind == PersonaData.PersonaDataField.Kind.DateOfBirth }?.let {
            val nameValue = it.value as PersonaData.PersonaDataField.DateOfBirth
            IdentifiedEntry.init(nameValue, it.id)
        },
        companyName = fields.firstOrNull { it.value.kind == PersonaData.PersonaDataField.Kind.CompanyName }?.let {
            val nameValue = it.value as PersonaData.PersonaDataField.CompanyName
            IdentifiedEntry.init(nameValue, it.id)
        },
        emailAddresses = fields.filter { it.value.kind == PersonaData.PersonaDataField.Kind.EmailAddress }.map {
            val emailValue = it.value as PersonaData.PersonaDataField.Email
            IdentifiedEntry.init(emailValue, it.id)
        },
        phoneNumbers = fields.filter { it.value.kind == PersonaData.PersonaDataField.Kind.PhoneNumber }.map {
            val emailValue = it.value as PersonaData.PersonaDataField.PhoneNumber
            IdentifiedEntry.init(emailValue, it.id)
        },
        urls = fields.filter { it.value.kind == PersonaData.PersonaDataField.Kind.Url }.map {
            val emailValue = it.value as PersonaData.PersonaDataField.Url
            IdentifiedEntry.init(emailValue, it.id)
        },
        postalAddresses = fields.filter { it.value.kind == PersonaData.PersonaDataField.Kind.PostalAddress }.map {
            val emailValue = it.value as PersonaData.PersonaDataField.PostalAddress
            IdentifiedEntry.init(emailValue, it.id)
        },
        creditCards = fields.filter { it.value.kind == PersonaData.PersonaDataField.Kind.CreditCard }.map {
            val emailValue = it.value as PersonaData.PersonaDataField.CreditCard
            IdentifiedEntry.init(emailValue, it.id)
        }
    )
}
