package com.babylon.wallet.android.data.dapp.model

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
