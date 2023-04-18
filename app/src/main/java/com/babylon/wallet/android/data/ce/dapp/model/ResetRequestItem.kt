package com.babylon.wallet.android.data.ce.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResetRequestItem(
    @SerialName("accounts")
    val accounts: Boolean,
    @SerialName("personaData")
    val personaData: Boolean
)

fun ResetRequestItem.toDomainModel(): MessageFromDataChannel.IncomingRequest.ResetRequestItem {
    return MessageFromDataChannel.IncomingRequest.ResetRequestItem(accounts, personaData)
}
