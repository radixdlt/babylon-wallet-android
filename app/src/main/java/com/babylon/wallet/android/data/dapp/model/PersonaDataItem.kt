package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// REQUEST
@Serializable
data class PersonaDataRequestItem( // REQUEST
    @SerialName("fields") val fields: List<PersonaData.PersonaDataField>
)

fun PersonaDataRequestItem.toDomainModel(isOngoing: Boolean = false): MessageFromDataChannel.IncomingRequest.PersonaRequestItem? {
    if (fields.isEmpty()) return null
    return MessageFromDataChannel.IncomingRequest.PersonaRequestItem(fields, isOngoing = isOngoing)
}

// RESPONSE
@Serializable
data class PersonaDataRequestResponseItem( // RESPONSE
    @SerialName("fields") val fields: List<PersonaData>
)

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