package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LedgerInteraction

@Serializable
enum class Curve {
    @SerialName("curve25519")
    Curve25519,

    @SerialName("secp256k1")
    Secp256k1
}

@Serializable
@SerialName("getDeviceInfo")
data class GetDeviceInfoRequest(
    @SerialName("interactionId")
    val interactionId: String,
) : LedgerInteraction

@Serializable
@SerialName("derivePublicKey")
data class DerivePublicKeyRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("keyParameters")
    val keyParameters: KeyParameters,
    @SerialName("ledgerDevice")
    val ledgerDevice: LedgerDevice,
) : LedgerInteraction {

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
@SerialName("importOlympiaDevice")
data class ImportOlympiaDeviceRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("derivationPaths")
    val derivationPaths: List<String>,
) : LedgerInteraction

@Serializable
@SerialName("signTransaction")
data class SignTransactionRequest(
    @SerialName("interactionId")
    val interactionId: String,
    @SerialName("signers")
    val signers: DerivePublicKeyRequest.KeyParameters,
    @SerialName("ledgerDevice")
    val ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
    @SerialName("compiledTransactionIntent")
    val compiledTransactionIntent: String,
    @SerialName("mode")
    val mode: Mode,
) : LedgerInteraction {

    @Serializable
    enum class Mode {
        @SerialName("verbose")
        Verbose,

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
    val signers: DerivePublicKeyRequest.KeyParameters,
    @SerialName("ledgerDevice")
    val ledgerDevice: DerivePublicKeyRequest.LedgerDevice,
    @SerialName("challenge")
    val challenge: String,
) : LedgerInteraction

@Serializable
enum class LedgerDeviceModel {
    @SerialName("nanoS")
    NanoS,

    @SerialName("nanoS+")
    NanoSPlus,

    @SerialName("nanoX")
    NanoX
}
