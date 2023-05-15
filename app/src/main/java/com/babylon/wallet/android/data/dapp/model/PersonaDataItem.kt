package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.pernetwork.Network

// REQUEST
@Serializable
data class PersonaDataRequestItem( // REQUEST
    @SerialName("fields") val fields: List<PersonaData.PersonaDataField>
)

fun PersonaDataRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.PersonaRequestItem? {
    if (fields.isEmpty()) return null
    return MessageFromDataChannel.IncomingRequest.PersonaRequestItem(fields, isOngoing = false)
}

// RESPONSE
@Serializable
data class PersonaDataRequestResponseItem( // RESPONSE
    @SerialName("fields") val fields: List<PersonaData>
)

fun List<Network.Persona.Field>.toDataModel(): PersonaDataRequestResponseItem? {
    if (this.isEmpty()) {
        return null
    }

    val personasData = map { networkPersonaField ->
        PersonaData(
            field = networkPersonaField.id.toPersonaDataField(),
            value = networkPersonaField.value
        )
    }

    return PersonaDataRequestResponseItem(
        fields = personasData
    )
}

@Serializable
data class PersonaData(
    @SerialName("field") val field: PersonaDataField,
    @SerialName("value") val value: String,
) {
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
}

fun PersonaData.PersonaDataField.toKind(): Network.Persona.Field.ID {
    return when (this) {
        PersonaData.PersonaDataField.GivenName -> Network.Persona.Field.ID.GivenName
        PersonaData.PersonaDataField.FamilyName -> Network.Persona.Field.ID.FamilyName
        PersonaData.PersonaDataField.EmailAddress -> Network.Persona.Field.ID.EmailAddress
        PersonaData.PersonaDataField.PhoneNumber -> Network.Persona.Field.ID.PhoneNumber
    }
}

fun Network.Persona.Field.ID.toPersonaDataField(): PersonaData.PersonaDataField {
    return when (this) {
        Network.Persona.Field.ID.GivenName -> PersonaData.PersonaDataField.GivenName
        Network.Persona.Field.ID.FamilyName -> PersonaData.PersonaDataField.FamilyName
        Network.Persona.Field.ID.EmailAddress -> PersonaData.PersonaDataField.EmailAddress
        Network.Persona.Field.ID.PhoneNumber -> PersonaData.PersonaDataField.PhoneNumber
    }
}
