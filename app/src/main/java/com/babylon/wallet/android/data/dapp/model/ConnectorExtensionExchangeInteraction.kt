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

    /**
     * The link was removed on the side that sends it, so the other side removes it too. Either side sends it: the
     * wallet when its user removed the connector, the connector extension when its user forgot the wallet.
     */
    @Serializable
    @SerialName("linkRemoved")
    data object LinkRemoved : ConnectorExtensionExchangeInteraction
}
