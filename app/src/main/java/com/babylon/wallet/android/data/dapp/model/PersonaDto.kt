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

fun PersonaDataField.toKind(): Network.Persona.Field.Kind {
    return when (this) {
        PersonaDataField.GivenName -> Network.Persona.Field.Kind.GivenName
        PersonaDataField.FamilyName -> Network.Persona.Field.Kind.FamilyName
        PersonaDataField.EmailAddress -> Network.Persona.Field.Kind.EmailAddress
        PersonaDataField.PhoneNumber -> Network.Persona.Field.Kind.PhoneNumber
    }
}

fun Network.Persona.Field.Kind.toPersonaDataField(): PersonaDataField {
    return when (this) {
        Network.Persona.Field.Kind.GivenName -> PersonaDataField.GivenName
        Network.Persona.Field.Kind.FamilyName -> PersonaDataField.FamilyName
        Network.Persona.Field.Kind.EmailAddress -> PersonaDataField.EmailAddress
        Network.Persona.Field.Kind.PhoneNumber -> PersonaDataField.PhoneNumber
    }
}
