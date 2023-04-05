package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimeAccountsRequestItem(
    @SerialName("requiresProofOfOwnership") val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts") val numberOfAccounts: NumberOfAccounts,
)

@Serializable
data class OngoingAccountsRequestItem(
    @SerialName("requiresProofOfOwnership") val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts") val numberOfAccounts: NumberOfAccounts,
)

@Serializable
data class NumberOfAccounts(
    @SerialName("quantifier") val quantifier: AccountNumberQuantifier,
    @SerialName("quantity") val quantity: Int,
) {

    @Serializable
    enum class AccountNumberQuantifier {
        @SerialName("exactly")
        Exactly,

        @SerialName("atLeast")
        AtLeast,
    }
}

fun NumberOfAccounts.AccountNumberQuantifier.toDomainModel(): IncomingRequest.AccountsRequestItem.AccountNumberQuantifier {
    return when (this) {
        NumberOfAccounts.AccountNumberQuantifier.Exactly -> {
            IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
        }
        NumberOfAccounts.AccountNumberQuantifier.AtLeast -> {
            IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        }
    }
}

fun OneTimeAccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity == 0 && numberOfAccounts.quantifier == NumberOfAccounts.AccountNumberQuantifier.Exactly) return null
    return IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = requiresProofOfOwnership,
        numberOfAccounts = numberOfAccounts.quantity,
        quantifier = numberOfAccounts.quantifier.toDomainModel()
    )
}

fun OngoingAccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity == 0 && numberOfAccounts.quantifier == NumberOfAccounts.AccountNumberQuantifier.Exactly) return null
    return IncomingRequest.AccountsRequestItem(
        isOngoing = true,
        requiresProofOfOwnership = requiresProofOfOwnership,
        numberOfAccounts = numberOfAccounts.quantity,
        quantifier = numberOfAccounts.quantifier.toDomainModel()
    )
}
