package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimePersonaDataRequestItem(
    @SerialName("fields")
    val fields: List<String>
)

@Serializable
data class OngoingPersonaDataRequestItem(
    @SerialName("fields")
    val fields: List<String>
)

fun OneTimePersonaDataRequestItem.toDomainModel(
    requestId: String,
    auth: MessageFromDataChannel.IncomingRequest.AuthRequest? = null
): MessageFromDataChannel.IncomingRequest.PersonaRequest {
    return MessageFromDataChannel.IncomingRequest.PersonaRequest(requestId, fields, false, auth)
}

fun OngoingPersonaDataRequestItem.toDomainModel(
    requestId: String,
    auth: MessageFromDataChannel.IncomingRequest.AuthRequest? = null
): MessageFromDataChannel.IncomingRequest.PersonaRequest {
    return MessageFromDataChannel.IncomingRequest.PersonaRequest(requestId, fields, true, auth)
}
