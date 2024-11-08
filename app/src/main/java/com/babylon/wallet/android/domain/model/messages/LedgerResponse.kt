package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletModel

sealed class LedgerResponse(val id: String) : IncomingMessage {

    data class DerivedPublicKey(
        val curve: Curve,
        val publicKeyHex: String,
        val derivationPath: String
    ) {
        enum class Curve {
            Curve25519, Secp256k1
        }
    }

    data class DerivedAddress(
        val derivedKey: DerivedPublicKey,
        val address: String
    )

    enum class LedgerDeviceModel {
        NanoS, NanoSPlus, NanoX;

        fun toProfileLedgerDeviceModel(): LedgerHardwareWalletModel {
            return when (this) {
                NanoS -> LedgerHardwareWalletModel.NANO_S
                NanoSPlus -> LedgerHardwareWalletModel.NANO_S_PLUS
                NanoX -> LedgerHardwareWalletModel.NANO_X
            }
        }
    }

    data class SignatureOfSigner(
        val derivedPublicKey: DerivedPublicKey,
        val signature: String,
    )

    data class GetDeviceInfoResponse(
        val interactionId: String,
        val model: LedgerDeviceModel,
        val deviceId: Exactly32Bytes
    ) : LedgerResponse(interactionId) {

        val factorSourceId: FactorSourceId.Hash
            get() = FactorSourceId.Hash(
                value = FactorSourceIdFromHash(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    body = deviceId
                )
            )
    }

    data class DerivePublicKeyResponse(
        val interactionId: String,
        val publicKeysHex: List<DerivedPublicKey>
    ) : LedgerResponse(interactionId)

    data class SignTransactionResponse(
        val interactionId: String,
        val signatures: List<SignatureOfSigner>
    ) : LedgerResponse(interactionId)

    data class SignSubintentHashResponse(
        val interactionId: String,
        val signatures: List<SignatureOfSigner>
    ) : LedgerResponse(interactionId)

    data class SignChallengeResponse(
        val interactionId: String,
        val signatures: List<SignatureOfSigner>
    ) : LedgerResponse(interactionId)

    data class DeriveAndDisplayAddressResponse(
        val interactionId: String,
        val derivedAddress: DerivedAddress
    ) : LedgerResponse(interactionId)

    data class LedgerErrorResponse(
        val interactionId: String,
        val code: LedgerErrorCode,
        val message: String
    ) : LedgerResponse(interactionId)
}
