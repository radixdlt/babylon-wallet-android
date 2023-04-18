package com.babylon.wallet.android.data.ce

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LedgerInteractionResponse : ConnectorExtensionInteraction

@Serializable
@SerialName("getDeviceInfo")
data class GetDeviceInfoResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: Success
) : LedgerInteractionResponse {

    @Serializable
    data class Success(
        @SerialName("model")
        val model: LedgerDeviceModel? = null,
        @SerialName("id")
        val id: String
    )
}

@Serializable
@SerialName("derivePublicKey")
data class DerivePublicKeyResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: Success
) : LedgerInteractionResponse {
    @Serializable
    data class Success(
        @SerialName("publicKey")
        val publicKeyHex: String
    )
}

@Serializable
@SerialName("importOlympiaDevice")
data class ImportOlympiaDeviceResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: Success
) : LedgerInteractionResponse {
    @Serializable
    data class Success(
        @SerialName("model")
        val model: LedgerDeviceModel,
        @SerialName("id")
        val id: String,
        @SerialName("derivedPublicKeys")
        val derivedPublicKeys: List<DerivedPublicKey>
    ) {
        @Serializable
        data class DerivedPublicKey(
            @SerialName("publicKey")
            val publicKey: String,
            @SerialName("path")
            val path: String
        )
    }
}

@Serializable
@SerialName("signTransaction")
data class SignTransactionResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: Success,
) : LedgerInteractionResponse {

    @Serializable
    data class Success(
        @SerialName("signature")
        val signatureHex: String,
        @SerialName("publicKey")
        val publixKeyHex: String
    )
}

@Serializable
@SerialName("signChallenge")
data class SignChallengeResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: Success
) : LedgerInteractionResponse {
    @Serializable
    data class Success(
        @SerialName("signature")
        val signatureHex: String,
        @SerialName("publicKey")
        val publixKeyHex: String
    )
}

@Serializable
@SerialName("error")
data class LedgerInteractionErrorResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("error")
    val error: Error
) : LedgerInteractionResponse {
    @Serializable
    data class Error(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String
    )
}

fun LedgerDeviceModel.toDomainModel(): MessageFromDataChannel.LedgerResponse.LedgerDeviceModel {
    return when (this) {
        LedgerDeviceModel.NanoS -> MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS
        LedgerDeviceModel.NanoSPlus -> MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoSPlus
        LedgerDeviceModel.NanoX -> MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoX
    }
}

fun ImportOlympiaDeviceResponse.Success.DerivedPublicKey.toDomainModel(): MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse.DerivedPublicKey {
    return MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse.DerivedPublicKey(publicKey, path)
}

fun LedgerInteractionResponse.toDomainModel(): MessageFromDataChannel {
    return when (this) {
        is DerivePublicKeyResponse -> MessageFromDataChannel.LedgerResponse.DerivePublicKeyResponse(
            interactionId,
            success.publicKeyHex
        )
        is GetDeviceInfoResponse -> MessageFromDataChannel.LedgerResponse.GetDeviceInfoResponse(
            interactionId,
            success.model?.toDomainModel() ?: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel.NanoS,
            success.id
        )
        is ImportOlympiaDeviceResponse -> MessageFromDataChannel.LedgerResponse.ImportOlympiaDeviceResponse(
            interactionId,
            success.model.toDomainModel(),
            success.id,
            success.derivedPublicKeys.map { it.toDomainModel() }
        )
        is LedgerInteractionErrorResponse -> MessageFromDataChannel.LedgerResponse.LedgerErrorResponse(
            interactionId,
            error.code,
            error.message
        )
        is SignChallengeResponse -> MessageFromDataChannel.LedgerResponse.SignChallengeResponse(
            interactionId,
            success.signatureHex,
            success.publixKeyHex
        )
        is SignTransactionResponse -> MessageFromDataChannel.LedgerResponse.SignTransactionResponse(
            interactionId,
            success.signatureHex,
            success.publixKeyHex
        )
    }
}