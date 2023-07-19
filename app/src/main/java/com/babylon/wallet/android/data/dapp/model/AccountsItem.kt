package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// REQUEST
@Serializable
data class AccountsRequestItem(
    @SerialName("challenge") val challenge: String? = null,
    @SerialName("numberOfAccounts") val numberOfAccounts: NumberOfValues
)

fun AccountsRequestItem.toDomainModel(isOngoing: Boolean = true): IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity == 0 &&
        numberOfAccounts.quantifier == NumberOfValues.Quantifier.Exactly
    ) {
        return null
    }
    return IncomingRequest.AccountsRequestItem(
        isOngoing = isOngoing,
        numberOfValues = numberOfAccounts.toDomainModel(),
        challenge = challenge
    )
}

fun NumberOfValues.Quantifier.toDomainModel(): IncomingRequest.NumberOfValues.Quantifier {
    return when (this) {
        NumberOfValues.Quantifier.Exactly -> {
            IncomingRequest.NumberOfValues.Quantifier.Exactly
        }

        NumberOfValues.Quantifier.AtLeast -> {
            IncomingRequest.NumberOfValues.Quantifier.AtLeast
        }
    }
}

// RESPONSE
// if challenge in request was set, challenge AND proofs MUST BOTH be set in response.
// Where ofc proofs and accounts must have same length and reference same accounts…
@Serializable
data class AccountsRequestResponseItem(
    @SerialName("accounts") val accounts: List<Account>,
    @SerialName("challenge") val challenge: String? = null,
    @SerialName("proofs") val proofs: List<AccountProof>? = null
) {

    @Serializable
    data class Account(
        @SerialName("address") val address: String,
        @SerialName("label") val label: String,
        @SerialName("appearanceId") val appearanceId: Int,
    )
}
