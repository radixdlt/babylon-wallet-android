package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val oneTimeAccounts: OneTimeAccountsRequestResponseItem?,
    @SerialName("ongoingAccounts")
    val ongoingAccounts: OngoingAccountsRequestResponseItem?,
    @SerialName("oneTimePersonaData")
    val oneTimePersonaData: OneTimePersonaDataRequestResponseItem?,
    @SerialName("ongoingPersonaData")
    val ongoingPersonaData: OngoingPersonaDataRequestResponseItem?
) : WalletRequestResponseItems()

@Serializable
sealed class WalletRequestResponseItems : WalletInteractionResponseItems()

@Serializable
@SerialName("transaction")
data class WalletTransactionResponseItems(
    @SerialName("send")
    val send: SendTransactionResponseItem
) : WalletInteractionResponseItems()

@Serializable
@Suppress("UnnecessaryAbstractClass")
abstract class WalletInteractionResponseItems

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
abstract class WalletInteractionResponse
