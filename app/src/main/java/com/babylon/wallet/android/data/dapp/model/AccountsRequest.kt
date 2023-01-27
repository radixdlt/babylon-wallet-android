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

fun OneTimeAccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem {
    return IncomingRequest.AccountsRequestItem(
        isOngoing = false,
        requiresProofOfOwnership = requiresProofOfOwnership,
        numberOfAccounts = numberOfAccounts.quantity,
        quantifier = numberOfAccounts.quantifier.toDomainModel()
    )
}

fun OngoingAccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem {
    return IncomingRequest.AccountsRequestItem(
        isOngoing = true,
        requiresProofOfOwnership = requiresProofOfOwnership,
        numberOfAccounts = numberOfAccounts.quantity,
        quantifier = numberOfAccounts.quantifier.toDomainModel()
    )
}
