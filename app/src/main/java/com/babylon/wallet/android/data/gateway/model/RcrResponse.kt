package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("method")
sealed class RcrResponse

@Serializable
@SerialName("sendRequest")
data class ResponseSendRequest(
    @SerialName("data")
    val data: DataResponseStatus,
    @SerialName("status")
    val status: Int
) : RcrRequest()

@Serializable
@SerialName("getRequests")
data class ResponseGetRequests(
    @SerialName("data")
    val data: List<String>,
    @SerialName("status")
    val status: Int,
) : RcrRequest()

@Serializable
@SerialName("sendResponse")
data class ResponseSendResponse(
    @SerialName("data")
    val data: DataResponseStatus,
    @SerialName("status")
    val status: Int
) : RcrRequest()

@Serializable
@SerialName("getResponses")
data class ResponseGetResponses(
    @SerialName("data")
    val data: List<String>,
    @SerialName("status")
    val status: Int
) : RcrRequest()

@Serializable
data class DataResponseStatus(
    @SerialName("ok")
    val ok: Boolean
)