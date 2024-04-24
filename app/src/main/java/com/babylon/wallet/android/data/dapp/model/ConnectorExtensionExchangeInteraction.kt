package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ConnectorExtensionExchangeInteraction(
    @SerialName("discriminator")
    val discriminator: String
) {

    @Serializable
    data class AccountList(
        @SerialName("accounts")
        val accounts: List<Account>
    ) : ConnectorExtensionExchangeInteraction("accountList")
}
