package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.bip32CanonicalString
import com.radixdlt.sargon.extensions.curve
import com.radixdlt.sargon.extensions.hex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("discriminator")
sealed interface LedgerInteractionRequest {
    val interactionId: String

    @Serializable
    @SerialName("getDeviceInfo")
    data class GetDeviceInfo(
        @SerialName("interactionId")
        override val interactionId: String,
    ) : LedgerInteractionRequest

    @Serializable
    @SerialName("derivePublicKeys")
    data class DerivePublicKeys(
        @SerialName("interactionId")
        override val interactionId: String,
        @SerialName("keysParameters")
        val keysParameters: List<KeyParameters>,
        @SerialName("ledgerDevice")
        val ledgerDevice: LedgerDevice,
    ) : LedgerInteractionRequest

    @Serializable
    @SerialName("signTransaction")
    data class SignTransaction(
        @SerialName("interactionId")
        override val interactionId: String,
        @SerialName("signers")
        val signers: List<KeyParameters>,
        @SerialName("ledgerDevice")
        val ledgerDevice: LedgerDevice,
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
    @SerialName("signSubintentHash")
    data class SignSubintentHash(
        @SerialName("interactionId")
        override val interactionId: String,
        @SerialName("signers")
        val signers: List<KeyParameters>,
        @SerialName("ledgerDevice")
        val ledgerDevice: LedgerDevice,
        @SerialName("subintentHash")
        val subintentHash: String,
    ) : LedgerInteractionRequest

    @Serializable
    @SerialName("signChallenge")
    data class SignChallenge(
        @SerialName("interactionId")
        override val interactionId: String,
        @SerialName("signers")
        val signers: List<KeyParameters>,
        @SerialName("ledgerDevice")
        val ledgerDevice: LedgerDevice,
        @SerialName("challenge")
        val challengeHex: String,
        @SerialName("origin")
        val origin: String,
        @SerialName("dAppDefinitionAddress")
        val dAppDefinitionAddress: String,
    ) : LedgerInteractionRequest

    @Serializable
    @SerialName("deriveAndDisplayAddress")
    data class DeriveAndDisplayAddress(
        @SerialName("interactionId")
        override val interactionId: String,
        @SerialName("keyParameters")
        val keyParameters: KeyParameters,
        @SerialName("ledgerDevice")
        val ledgerDevice: LedgerDevice
    ) : LedgerInteractionRequest

    @Serializable
    data class LedgerDevice(
        @SerialName("name")
        val name: String?,
        @SerialName("model")
        val model: LedgerDeviceModel,
        @SerialName("id")
        val id: String
    ) {

        companion object {
            fun from(factorSource: FactorSource.Ledger): LedgerDevice = LedgerDevice(
                id = factorSource.value.id.body.hex,
                name = factorSource.value.hint.label,
                model = LedgerDeviceModel.from(factorSource.value.hint.model),
            )
        }
    }

    @Serializable
    data class KeyParameters(
        @SerialName("curve")
        val curve: Curve,
        @SerialName("derivationPath")
        val derivationPath: String
    ) {

        companion object {

            fun from(derivationPath: DerivationPath) = KeyParameters(
                curve = Curve.from(derivationPath.curve),
                derivationPath = derivationPath.bip32CanonicalString
            )
        }
    }
}

@Serializable
enum class Curve {
    @SerialName("curve25519")
    Curve25519,

    @SerialName("secp256k1")
    Secp256k1;

    companion object {
        fun from(publicKey: PublicKey): Curve = when (publicKey) {
            is PublicKey.Ed25519 -> Curve25519
            is PublicKey.Secp256k1 -> Secp256k1
        }

        fun from(curve: Slip10Curve): Curve = when (curve) {
            Slip10Curve.CURVE25519 -> Curve25519
            Slip10Curve.SECP256K1 -> Secp256k1
        }
    }
}

@Serializable
enum class LedgerDeviceModel {
    @SerialName("nanoS")
    NanoS,

    @SerialName("nanoS+")
    NanoSPlus,

    @SerialName("nanoX")
    NanoX;

    companion object {
        fun from(model: LedgerHardwareWalletModel): LedgerDeviceModel = when (model) {
            LedgerHardwareWalletModel.NANO_S -> NanoS
            LedgerHardwareWalletModel.NANO_S_PLUS -> NanoSPlus
            LedgerHardwareWalletModel.NANO_X -> NanoX
        }
    }
}
