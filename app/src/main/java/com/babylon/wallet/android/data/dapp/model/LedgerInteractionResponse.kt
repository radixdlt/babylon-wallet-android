package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.LedgerResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LedgerInteractionResponse : ConnectorExtensionInteraction

@Serializable
data class DerivedPublicKey(
    @SerialName("curve")
    val curve: Curve,
    @SerialName("publicKey")
    val publicKey: String,
    @SerialName("derivationPath")
    val derivationPath: String
)

@Serializable
data class SignatureOfSigner(
    @SerialName("derivedPublicKey")
    val derivedPublicKey: DerivedPublicKey,
    @SerialName("signature")
    val signature: String,
)

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
@SerialName("derivePublicKeys")
data class DerivePublicKeyResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: List<DerivedPublicKey>
) : LedgerInteractionResponse

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
    )
}

@Serializable
@SerialName("signTransaction")
data class SignTransactionResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: List<SignatureOfSigner>,
) : LedgerInteractionResponse

@Serializable
@SerialName("signChallenge")
data class SignChallengeResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: List<SignatureOfSigner>
) : LedgerInteractionResponse

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

fun Curve.toDomainModel(): LedgerResponse.DerivedPublicKey.Curve {
    return when (this) {
        Curve.Curve25519 -> LedgerResponse.DerivedPublicKey.Curve.Curve25519
        Curve.Secp256k1 -> LedgerResponse.DerivedPublicKey.Curve.Secp256k1
    }
}

fun SignatureOfSigner.toDomainModel(): LedgerResponse.SignatureOfSigner {
    return LedgerResponse.SignatureOfSigner(
        derivedPublicKey = derivedPublicKey.toDomainModel(),
        signature = signature,
    )
}

fun LedgerDeviceModel.toDomainModel(): LedgerResponse.LedgerDeviceModel {
    return when (this) {
        LedgerDeviceModel.NanoS -> LedgerResponse.LedgerDeviceModel.NanoS
        LedgerDeviceModel.NanoSPlus -> LedgerResponse.LedgerDeviceModel.NanoSPlus
        LedgerDeviceModel.NanoX -> LedgerResponse.LedgerDeviceModel.NanoX
    }
}

fun DerivedPublicKey.toDomainModel(): LedgerResponse.DerivedPublicKey {
    return LedgerResponse.DerivedPublicKey(
        curve = curve.toDomainModel(),
        publicKeyHex = publicKey,
        derivationPath = derivationPath
    )
}

fun List<DerivedPublicKey>.toDomainModel() = map { derivedPublicKey ->
    derivedPublicKey.toDomainModel()
}

fun LedgerInteractionResponse.toDomainModel(): MessageFromDataChannel {
    return when (this) {
        is DerivePublicKeyResponse -> LedgerResponse.DerivePublicKeyResponse(
            interactionId,
            success.toDomainModel().map {
                it.publicKeyHex
            }
        )
        is GetDeviceInfoResponse -> LedgerResponse.GetDeviceInfoResponse(
            interactionId,
            success.model?.toDomainModel() ?: LedgerResponse.LedgerDeviceModel.NanoS,
            success.id
        )
        is ImportOlympiaDeviceResponse -> LedgerResponse.ImportOlympiaDeviceResponse(
            interactionId,
            success.model.toDomainModel(),
            success.id,
            success.derivedPublicKeys.map { it.toDomainModel() }
        )
        is LedgerInteractionErrorResponse -> LedgerResponse.LedgerErrorResponse(
            interactionId,
            error.code,
            error.message
        )
        is SignChallengeResponse -> LedgerResponse.SignChallengeResponse(
            interactionId,
            success.map { it.toDomainModel() }
        )
        is SignTransactionResponse -> LedgerResponse.SignTransactionResponse(
            interactionId,
            success.map { it.toDomainModel() }
        )
    }
}
