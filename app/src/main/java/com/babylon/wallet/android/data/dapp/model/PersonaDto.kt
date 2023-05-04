package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.pernetwork.Network

@Serializable
data class PersonaDto(
    @SerialName("identityAddress") val identityAddress: String,
    @SerialName("label") val label: String,
)

@Serializable
data class PersonaData(
    @SerialName("field") val field: PersonaDataField,
    @SerialName("value") val value: String,
)

@Serializable
enum class PersonaDataField {
    @SerialName("givenName")
    GivenName,

    @SerialName("familyName")
    FamilyName,

    @SerialName("emailAddress")
    EmailAddress,

    @SerialName("phoneNumber")
    PhoneNumber
}

fun PersonaDataField.toKind(): Network.Persona.Field.ID {
    return when (this) {
        PersonaDataField.GivenName -> Network.Persona.Field.ID.GivenName
        PersonaDataField.FamilyName -> Network.Persona.Field.ID.FamilyName
        PersonaDataField.EmailAddress -> Network.Persona.Field.ID.EmailAddress
        PersonaDataField.PhoneNumber -> Network.Persona.Field.ID.PhoneNumber
    }
}

fun Network.Persona.Field.ID.toPersonaDataField(): PersonaDataField {
    return when (this) {
        Network.Persona.Field.ID.GivenName -> PersonaDataField.GivenName
        Network.Persona.Field.ID.FamilyName -> PersonaDataField.FamilyName
        Network.Persona.Field.ID.EmailAddress -> PersonaDataField.EmailAddress
        Network.Persona.Field.ID.PhoneNumber -> PersonaDataField.PhoneNumber
    }
}
