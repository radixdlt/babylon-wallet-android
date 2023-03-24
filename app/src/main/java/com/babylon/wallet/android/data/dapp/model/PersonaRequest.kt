package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimePersonaDataRequestItem(
    @SerialName("fields")
    val fields: List<PersonaDataField>
)

@Serializable
data class OngoingPersonaDataRequestItem(
    @SerialName("fields")
    val fields: List<PersonaDataField>
)

fun OneTimePersonaDataRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.PersonaRequestItem {
    return MessageFromDataChannel.IncomingRequest.PersonaRequestItem(fields, false)
}

fun OngoingPersonaDataRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.PersonaRequestItem {
    return MessageFromDataChannel.IncomingRequest.PersonaRequestItem(fields, true)
}
