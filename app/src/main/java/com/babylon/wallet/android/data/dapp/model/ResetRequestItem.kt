package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.IncomingMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResetRequestItem(
    @SerialName("accounts")
    val accounts: Boolean,
    @SerialName("personaData")
    val personaData: Boolean
)

fun ResetRequestItem.toDomainModel(): IncomingMessage.IncomingRequest.AuthorizedRequest.ResetRequestItem {
    return IncomingMessage.IncomingRequest.AuthorizedRequest.ResetRequestItem(accounts, personaData)
}
