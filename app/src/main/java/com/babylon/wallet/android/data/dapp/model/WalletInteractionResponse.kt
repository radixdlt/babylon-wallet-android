@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@SerialName("unauthorizedRequest")
data class WalletUnauthorizedRequestResponseItems(
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestResponseItem? = null,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestResponseItem? = null
) : WalletRequestResponseItems()

@Serializable
@SerialName("authorizedRequest")
data class WalletAuthorizedRequestResponseItems(
    @SerialName("auth")
    val auth: AuthRequestResponseItem?,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestResponseItem? = null,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: OngoingAccountsRequestResponseItem? = null,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestResponseItem? = null,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: OngoingPersonaDataRequestResponseItem? = null
) : WalletRequestResponseItems()

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class WalletRequestResponseItems : WalletInteractionResponseItems()

@Serializable
@SerialName("transaction")
data class WalletTransactionResponseItems(
    @SerialName("send")
    val send: SendTransactionResponseItem
) : WalletInteractionResponseItems()

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
@SerialName("failure")
data class WalletInteractionFailureResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("error")
    val error: WalletErrorType,
    @SerialName("message")
    val message: String? = null,
) : WalletInteractionResponse()

@Suppress("UnnecessaryAbstractClass")
@Serializable
@JsonClassDiscriminator("discriminator")
sealed class WalletInteractionResponse
