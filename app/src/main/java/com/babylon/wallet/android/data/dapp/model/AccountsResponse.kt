package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimeAccountsWithProofOfOwnershipResponseItem(
    override val requestType: String,
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : WalletResponseItem()

@Serializable
data class OneTimeAccountsWithoutProofOfOwnershipResponseItem(
    override val requestType: String,
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : WalletResponseItem()

enum class OneTimeAccountsRequestType(val requestType: String) {
    ONE_TIME_ACCOUNTS_READ("oneTimeAccountsRead"),
    ONGOING_ACCOUNTS_READ("ongoingAccountsRead")
}
