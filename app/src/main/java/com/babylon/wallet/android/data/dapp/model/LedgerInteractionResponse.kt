package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.LedgerResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import rdx.works.core.HexCoded32Bytes

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
    val success: Success? = null,
    @SerialName("error")
    val error: Error? = null
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
    val success: List<DerivedPublicKey>? = null,
    @SerialName("error")
    val error: Error? = null
) : LedgerInteractionResponse

@Serializable
@SerialName("signTransaction")
data class SignTransactionResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: List<SignatureOfSigner>? = null,
    @SerialName("error")
    val error: Error? = null
) : LedgerInteractionResponse

@Serializable
@SerialName("signChallenge")
data class SignChallengeResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: List<SignatureOfSigner>? = null,
    @SerialName("error")
    val error: Error? = null
) : LedgerInteractionResponse

@Serializable
@SerialName("deriveAndDisplayAddress")
data class DeriveAndDisplayAddressResponse(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("success")
    val success: DerivedAddress? = null,
    @SerialName("error")
    val error: Error? = null
) : LedgerInteractionResponse

@Serializable
data class DerivedAddress(
    val derivedKey: DerivedPublicKey,
    val address: String
)

@Serializable
data class Error(
    @SerialName("code")
    @Serializable(with = LedgerErrorCodeSerializer::class)
    val code: LedgerErrorCode? = null,
    @SerialName("message")
    val message: String
)

@Serializable
enum class LedgerErrorCode {
    @SerialName("0")
    Generic,

    @SerialName("1")
    BlindSigningNotEnabledButRequired,

    @SerialName("2")
    UserRejectedSigningOfTransaction
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

@Suppress("SwallowedException")
fun LedgerInteractionResponse.toDomainModel(): MessageFromDataChannel {
    try {
        return when (this) {
            is DerivePublicKeyResponse -> toDomainModel()
            is GetDeviceInfoResponse -> toDomainModel()
            is SignChallengeResponse -> toDomainModel()
            is SignTransactionResponse -> toDomainModel()
            is DeriveAndDisplayAddressResponse -> toDomainModel()
        }
    } catch (e: Exception) {
        throw RadixWalletException.IncomingMessageException.LedgerResponseParse(e)
    }
}

private fun SignTransactionResponse.toDomainModel() =
    if (success != null) {
        LedgerResponse.SignTransactionResponse(
            interactionId,
            success.map { it.toDomainModel() }
        )
    } else {
        LedgerResponse.LedgerErrorResponse(
            interactionId = interactionId,
            code = error?.code ?: LedgerErrorCode.Generic,
            message = error?.message.orEmpty()
        )
    }

private fun SignChallengeResponse.toDomainModel() =
    if (success != null) {
        LedgerResponse.SignChallengeResponse(
            interactionId,
            success.map { it.toDomainModel() }
        )
    } else {
        LedgerResponse.LedgerErrorResponse(
            interactionId = interactionId,
            code = error?.code ?: LedgerErrorCode.Generic,
            message = error?.message.orEmpty()
        )
    }

private fun GetDeviceInfoResponse.toDomainModel() =
    if (success != null) {
        LedgerResponse.GetDeviceInfoResponse(
            interactionId = interactionId,
            model = success.model?.toDomainModel() ?: LedgerResponse.LedgerDeviceModel.NanoS,
            deviceId = HexCoded32Bytes(success.id)
        )
    } else {
        LedgerResponse.LedgerErrorResponse(
            interactionId = interactionId,
            code = error?.code ?: LedgerErrorCode.Generic,
            message = error?.message.orEmpty()
        )
    }

private fun DerivePublicKeyResponse.toDomainModel() =
    if (success != null) {
        LedgerResponse.DerivePublicKeyResponse(
            interactionId,
            success.toDomainModel()
        )
    } else {
        LedgerResponse.LedgerErrorResponse(
            interactionId = interactionId,
            code = error?.code ?: LedgerErrorCode.Generic,
            message = error?.message.orEmpty()
        )
    }

private fun DeriveAndDisplayAddressResponse.toDomainModel() = if (success != null) {
    LedgerResponse.DeriveAndDisplayAddressResponse(
        interactionId = interactionId,
        derivedAddress = LedgerResponse.DerivedAddress(
            derivedKey = success.derivedKey.toDomainModel(),
            address = success.address
        )
    )
} else {
    LedgerResponse.LedgerErrorResponse(
        interactionId = interactionId,
        code = error?.code ?: LedgerErrorCode.Generic,
        message = error?.message.orEmpty()
    )
}

object LedgerErrorCodeSerializer : KSerializer<LedgerErrorCode> {

    private val delegate = LedgerErrorCode.serializer()

    override val descriptor: SerialDescriptor
        get() = delegate.descriptor

    @Suppress("SwallowedException")
    override fun deserialize(decoder: Decoder): LedgerErrorCode {
        return try {
            decoder.decodeSerializableValue(delegate)
        } catch (e: Exception) {
            LedgerErrorCode.Generic
        }
    }

    override fun serialize(encoder: Encoder, value: LedgerErrorCode) {
        encoder.encodeSerializableValue(delegate, value)
    }
}
