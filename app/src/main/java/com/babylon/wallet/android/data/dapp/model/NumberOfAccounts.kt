package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NumberOfAccounts(
    @SerialName("quantifier") val quantifier: AccountNumberQuantifier,
    @SerialName("quantity") val quantity: Int,
)

@Serializable
enum class AccountNumberQuantifier {
    @SerialName("exactly")
    Exactly,

    @SerialName("atLeast")
    AtLeast,
}

fun AccountNumberQuantifier.toDomainModel(): MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier {
    return when (this) {
        AccountNumberQuantifier.Exactly -> MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
        AccountNumberQuantifier.AtLeast -> MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
    }
}
