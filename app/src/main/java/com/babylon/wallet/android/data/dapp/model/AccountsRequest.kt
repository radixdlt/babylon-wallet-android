package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimeAccountsRequestItem(
    @SerialName("requiresProofOfOwnership")
    val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts")
    val numberOfAccounts: NumberOfAccounts,
)

@Serializable
data class OngoingAccountsRequestItem(
    @SerialName("requiresProofOfOwnership")
    val requiresProofOfOwnership: Boolean,
    @SerialName("numberOfAccounts")
    val numberOfAccounts: NumberOfAccounts,
)

fun OneTimeAccountsRequestItem.toDomainModel(
    requestId: String,
    authRequest: IncomingRequest.AuthRequest? = null,
) = IncomingRequest.AccountsRequest(
    requestId = requestId,
    isOngoing = false,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts.quantity,
    quantifier = numberOfAccounts.quantifier.toDomainModel(),
    authRequest = authRequest
)

fun OngoingAccountsRequestItem.toDomainModel(
    requestId: String,
    authRequest: IncomingRequest.AuthRequest? = null,
) = IncomingRequest.AccountsRequest(
    requestId = requestId,
    isOngoing = true,
    requiresProofOfOwnership = requiresProofOfOwnership,
    numberOfAccounts = numberOfAccounts.quantity,
    quantifier = numberOfAccounts.quantifier.toDomainModel(),
    authRequest = authRequest
)
