package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
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

fun OneTimeAccountsReadRequestItem.toDomainModel(requestId: String) = IncomingRequest.AccountsRequest(
    requestId = requestId,
    isOngoing = false,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts ?: 0
)

fun OngoingAccountsReadRequestItem.toDomainModel(requestId: String) = IncomingRequest.AccountsRequest(
    requestId = requestId,
    isOngoing = true,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts ?: 1
)
