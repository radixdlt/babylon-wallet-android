@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("discriminator")
sealed interface ConnectorExtensionExchangeInteraction {

    @Serializable
    @SerialName("accountList")
    data class AccountList(
        @SerialName("accounts")
        val accounts: List<Account>
    ) : ConnectorExtensionExchangeInteraction
}
