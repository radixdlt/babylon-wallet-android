package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.data.dapp.model.AccountsRequestItem.NumberOfAccounts
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// REQUEST
@Serializable
data class AccountsRequestItem(
    @SerialName("challenge") val challenge: String? = null, // TODO HexString32Bytes
    @SerialName("numberOfAccounts") val numberOfAccounts: NumberOfAccounts
) {

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
}

fun AccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity == 0 &&
        numberOfAccounts.quantifier == NumberOfAccounts.AccountNumberQuantifier.Exactly
    ) {
        return null
    }

    return if (this.challenge == null) {
        IncomingRequest.AccountsRequestItem(
            isOngoing = false,
            requiresProofOfOwnership = true,
            numberOfAccounts = numberOfAccounts.quantity,
            quantifier = numberOfAccounts.quantifier.toDomainModel()
        )
    } else {
        IncomingRequest.AccountsRequestItem(
            isOngoing = false,
            requiresProofOfOwnership = false,
            numberOfAccounts = numberOfAccounts.quantity,
            quantifier = numberOfAccounts.quantifier.toDomainModel()
        )
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

// RESPONSE
// if challenge in request was set, challenge AND proofs MUST BOTH be set in response.
// Where ofc proofs and accounts must have same length and reference same accounts…
@Serializable
data class AccountsRequestResponseItem(
    @SerialName("accounts") val accounts: List<Account>,
    @SerialName("challenge") val challenge: String?, // TODO Challenge
    @SerialName("proofs") val proofs: List<AccountProof>?
) {

    @Serializable
    data class Account(
        @SerialName("address") val address: String,
        @SerialName("label") val label: String,
        @SerialName("appearanceId") val appearanceId: Int,
    )
}

fun List<AccountItemUiModel>.toDataModel(): AccountsRequestResponseItem? {
    if (this.isEmpty()) {
        return null
    }

    val accounts = map { accountItemUiModel ->
        AccountsRequestResponseItem.Account(
            address = accountItemUiModel.address,
            label = accountItemUiModel.displayName.orEmpty(),
            appearanceId = accountItemUiModel.appearanceID
        )
    }

    return AccountsRequestResponseItem(
        accounts = accounts,
        challenge = null, // TODO
        proofs = null // TODO
    )
}
