package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimeAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OneTimeAccountsRequestResponseItem()

@Serializable
data class OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OneTimeAccountsRequestResponseItem()

@Serializable
data class OngoingAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OngoingAccountsRequestResponseItem()

@Serializable
data class OngoingAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OngoingAccountsRequestResponseItem()

@Serializable
abstract class OngoingAccountsRequestResponseItem

@Serializable
abstract class OneTimeAccountsRequestResponseItem