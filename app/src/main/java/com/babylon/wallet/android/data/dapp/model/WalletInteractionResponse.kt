@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@SerialName("unauthorized")
data class WalletUnauthorizedRequestResponseItems(
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestResponseItem? = null,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestResponseItem? = null
) : WalletRequestResponseItems()

@Serializable
@SerialName("authorized")
data class WalletAuthorizedRequestResponseItems(
    @SerialName("auth")
    val auth: AuthRequestResponseItem?,
    @SerialName("oneTimeAccounts")
    val oneTimeAccounts: OneTimeAccountsRequestResponseItem?,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: OngoingAccountsRequestResponseItem?,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestResponseItem?,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: OngoingPersonaDataRequestResponseItem?
) : WalletRequestResponseItems()

@Serializable
@JsonClassDiscriminator("type")
sealed class WalletRequestResponseItems : WalletInteractionResponseItems()

@Serializable
data class WalletTransactionResponseItems(
    @SerialName("type")
    val type: String,
    @SerialName("sendTransaction")
    val sendTransaction: SendTransactionResponseItem
) : WalletInteractionResponseItems()

@Serializable
@JsonClassDiscriminator("type")
abstract class WalletInteractionResponseItems

@Serializable
data class WalletInteractionSuccessResponse(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("items")
    val items: WalletInteractionResponseItems
) : WalletInteractionResponse()

@Serializable
data class WalletInteractionFailureResponse(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("error")
    val error: WalletErrorType,
    @SerialName("message")
    val message: String? = null,
) : WalletInteractionResponse()

abstract class WalletInteractionResponse
