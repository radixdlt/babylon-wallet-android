@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Suppress("UnnecessaryAbstractClass")
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class WalletInteractionResponse

@Serializable
@JsonClassDiscriminator("discriminator")
@Suppress("UnnecessaryAbstractClass")
sealed class WalletInteractionResponseItems

@Serializable
@SerialName("success")
data class WalletInteractionSuccessResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("items")
    val items: WalletInteractionResponseItems
) : WalletInteractionResponse()

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class WalletRequestResponseItems : WalletInteractionResponseItems()

@Serializable
@SerialName("unauthorizedRequest")
data class WalletUnauthorizedRequestResponseItems(
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: AccountsRequestResponseItem? = null,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: PersonaDataRequestResponseItem? = null
) : WalletRequestResponseItems()

@Serializable
@SerialName("authorizedRequest")
data class WalletAuthorizedRequestResponseItems(
    @SerialName("auth")
    val auth: AuthRequestResponseItem?,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: AccountsRequestResponseItem? = null,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: AccountsRequestResponseItem? = null,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: PersonaDataRequestResponseItem? = null,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: PersonaDataRequestResponseItem? = null
) : WalletRequestResponseItems()

@Serializable
@SerialName("failure")
data class WalletInteractionFailureResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("error")
    val error: WalletErrorType,
    @SerialName("message")
    val message: String? = null,
) : WalletInteractionResponse()

@Serializable
@SerialName("transaction")
data class WalletTransactionResponseItems(
    @SerialName("send")
    val send: SendTransactionResponseItem
) : WalletInteractionResponseItems() {

    @Serializable
    data class SendTransactionResponseItem(
        @SerialName("transactionIntentHash")
        val transactionIntentHash: String,
    )
}
