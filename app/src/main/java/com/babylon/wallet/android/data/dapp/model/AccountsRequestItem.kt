@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class OneTimeAccountsRequestItem {
    @SerialName("numberOfAccounts")
    abstract val numberOfAccounts: NumberOfAccounts
}

@Serializable
@SerialName("oneTimeAccountsWithProofOfOwnership")
data class OneTimeAccountsWithProofOfOwnershipRequestItem(
    @SerialName("challenge") val challenge: String,
    override val numberOfAccounts: NumberOfAccounts,
) : OneTimeAccountsRequestItem()

@Serializable
@SerialName("oneTimeAccountsWithoutProofOfOwnership")
data class OneTimeAccountsWithoutProofOfOwnershipRequestItem(
    override val numberOfAccounts: NumberOfAccounts,
) : OneTimeAccountsRequestItem()

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class OngoingAccountsRequestItem {
    @SerialName("numberOfAccounts")
    abstract val numberOfAccounts: NumberOfAccounts
}

@Serializable
@SerialName("ongoingAccountsWithProofOfOwnership")
data class OngoingAccountsWithProofOfOwnershipRequestItem(
    @SerialName("challenge") val challenge: String,
    override val numberOfAccounts: NumberOfAccounts
) : OngoingAccountsRequestItem()

@Serializable
@SerialName("ongoingAccountsWithoutProofOfOwnership")
data class OngoingAccountsWithoutProofOfOwnershipRequestItem(
    override val numberOfAccounts: NumberOfAccounts
) : OngoingAccountsRequestItem()

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
    if (numberOfAccounts.quantity == 0 && numberOfAccounts.quantifier == NumberOfAccounts.AccountNumberQuantifier.Exactly) {
        return null
    }

    when (this) {
        is OneTimeAccountsWithProofOfOwnershipRequestItem -> {
            return IncomingRequest.AccountsRequestItem(
                isOngoing = false,
                requiresProofOfOwnership = false,
                numberOfAccounts = numberOfAccounts.quantity,
                quantifier = numberOfAccounts.quantifier.toDomainModel()
            )
        }

        is OneTimeAccountsWithoutProofOfOwnershipRequestItem -> {
            return IncomingRequest.AccountsRequestItem(
                isOngoing = false,
                requiresProofOfOwnership = true,
                numberOfAccounts = numberOfAccounts.quantity,
                quantifier = numberOfAccounts.quantifier.toDomainModel()
            )
        }
    }
}

fun OngoingAccountsRequestItem.toDomainModel(): IncomingRequest.AccountsRequestItem? {
    // correct request but not actionable, return null
    if (numberOfAccounts.quantity == 0 && numberOfAccounts.quantifier == NumberOfAccounts.AccountNumberQuantifier.Exactly) {
        return null
    }

    return when (this) {
        is OngoingAccountsWithProofOfOwnershipRequestItem -> {
            IncomingRequest.AccountsRequestItem(
                isOngoing = true,
                requiresProofOfOwnership = true,
                numberOfAccounts = numberOfAccounts.quantity,
                quantifier = numberOfAccounts.quantifier.toDomainModel()
            )
        }

        is OngoingAccountsWithoutProofOfOwnershipRequestItem -> {
            IncomingRequest.AccountsRequestItem(
                isOngoing = true,
                requiresProofOfOwnership = false,
                numberOfAccounts = numberOfAccounts.quantity,
                quantifier = numberOfAccounts.quantifier.toDomainModel()
            )
        }
    }
}
