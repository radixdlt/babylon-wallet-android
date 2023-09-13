package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve

@Serializable
sealed interface LedgerInteractionRequest

@Serializable
enum class Curve {
    @SerialName("curve25519")
    Curve25519,

    @SerialName("secp256k1")
    Secp256k1;

    companion object {
        fun from(curve: Slip10Curve): Curve {
            return when (curve) {
                Slip10Curve.CURVE_25519 -> Curve25519
                Slip10Curve.SECP_256K1 -> Secp256k1
            }
        }
    }
}

@Serializable
@SerialName("getDeviceInfo")
data class GetDeviceInfoRequest(
    @SerialName("interactionId")
    val interactionId: String,
) : LedgerInteractionRequest

@Serializable
@SerialName("derivePublicKeys")
data class DerivePublicKeyRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("keysParameters")
    val keysParameters: List<KeyParameters>,
    @SerialName("ledgerDevice")
    val ledgerDevice: LedgerDevice,
) : LedgerInteractionRequest {

    @Serializable
    data class KeyParameters(
        @SerialName("curve")
        val curve: Curve,
        @SerialName("derivationPath")
        val derivationPath: String
    )

    @Serializable
    data class LedgerDevice(
        @SerialName("name")
        val name: String?,
        @SerialName("model")
        val model: LedgerDeviceModel,
        @SerialName("id")
        val id: String
    )
}

@Serializable
@SerialName("signTransaction")
data class SignTransactionRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("signers")
    val signers: List<DerivePublicKeyRequest.KeyParameters>,
    @SerialName("ledgerDevice")
    val ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
    @SerialName("displayHash")
    val displayHash: Boolean,
    @SerialName("compiledTransactionIntent")
    val compiledTransactionIntent: String,
    @SerialName("mode")
    val mode: Mode,
) : LedgerInteractionRequest {

    @Serializable
    enum class Mode {
        @SerialName("summary")
        Summary
    }
}

@Serializable
@SerialName("signChallenge")
data class SignChallengeRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("signers")
    val signers: List<DerivePublicKeyRequest.KeyParameters>,
    @SerialName("ledgerDevice")
    val ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
    @SerialName("challenge")
    val challengeHex: String,
    @SerialName("origin")
    val origin: String,
    @SerialName("dAppDefinitionAddress")
    val dAppDefinitionAddress: String,
) : LedgerInteractionRequest

@Serializable
enum class LedgerDeviceModel {
    @SerialName("nanoS")
    NanoS,

    @SerialName("nanoS+")
    NanoSPlus,

    @SerialName("nanoX")
    NanoX;

    companion object {
        fun LedgerHardwareWalletFactorSource.getLedgerDeviceModel(): LedgerDeviceModel? {
            return when (this.hint.model) {
                LedgerHardwareWalletFactorSource.DeviceModel.NANO_S.value -> NanoS
                LedgerHardwareWalletFactorSource.DeviceModel.NANO_S_PLUS.value -> NanoSPlus
                LedgerHardwareWalletFactorSource.DeviceModel.NANO_X.value -> NanoX
                else -> null
            }
        }
    }
}
