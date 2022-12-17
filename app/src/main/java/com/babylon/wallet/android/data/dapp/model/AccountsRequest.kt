package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.IncomingRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("oneTimeAccountsRead")
data class OneTimeAccountsReadRequestItem(
    @SerialName("requiresProofOfOwnership")
    val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts")
    val numberOfAccounts: Int? = null
) : WalletRequestItem()

@Serializable
@SerialName("ongoingAccountsRead")
data class OngoingAccountsReadRequestItem(
    @SerialName("requiresProofOfOwnership")
    val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts")
    val numberOfAccounts: Int? = null
) : WalletRequestItem()

fun OneTimeAccountsReadRequestItem.toDomainModel() = IncomingRequest.AccountsRequest(
    isOngoing = false,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts ?: 1
)

fun OngoingAccountsReadRequestItem.toDomainModel() = IncomingRequest.AccountsRequest(
    isOngoing = true,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts ?: 1
)
