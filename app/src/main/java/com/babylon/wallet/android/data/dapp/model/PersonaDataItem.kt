package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// REQUEST
@Serializable
data class PersonaDataRequestItem(
    // REQUEST
    @SerialName("isRequestingName") val isRequestingName: Boolean? = null,
    @SerialName("numberOfRequestedEmailAddresses") val numberOfRequestedEmailAddresses: NumberOfValues? = null,
    @SerialName("numberOfRequestedPhoneNumbers") val numberOfRequestedPhoneNumbers: NumberOfValues? = null,
)

// RESPONSE
@Serializable
data class PersonaDataRequestResponseItem( // RESPONSE
    @SerialName("name") val name: PersonaDataName? = null,
    @SerialName("emailAddresses") val emailAddresses: List<String> = emptyList(),
    @SerialName("phoneNumbers") val phoneNumbers: List<String> = emptyList()
)

@Serializable
data class PersonaDataName(
    @SerialName("variant") val variant: Variant,
    @SerialName("familyName") val familyName: String,
    @SerialName("givenNames") val givenNames: String,
    @SerialName("nickname") val nickname: String,
) {
    @Serializable
    enum class Variant {
        @SerialName("eastern")
        Eastern,

        @SerialName("western")
        Western
    }
}

fun PersonaDataRequestItem.toDomainModel(isOngoing: Boolean = false): MessageFromDataChannel.IncomingRequest.PersonaRequestItem? {
    if (isRequestingName == null && numberOfRequestedPhoneNumbers == null && numberOfRequestedEmailAddresses == null) return null
    return MessageFromDataChannel.IncomingRequest.PersonaRequestItem(
        isRequestingName = isRequestingName == true,
        numberOfRequestedEmailAddresses = numberOfRequestedEmailAddresses?.toDomainModel(),
        numberOfRequestedPhoneNumbers = numberOfRequestedPhoneNumbers?.toDomainModel(),
        isOngoing = isOngoing
    )
}
