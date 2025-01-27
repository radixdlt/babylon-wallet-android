package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.bip32String
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapError
import rdx.works.core.sargon.init
import rdx.works.core.then
import rdx.works.profile.domain.UpdateFactorSourceLastUsedUseCase
import javax.inject.Inject

class AccessLedgerHardwareWalletFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val updateFactorSourceLastUsedUseCase: UpdateFactorSourceLastUsedUseCase
) : AccessFactorSource<FactorSource.Ledger> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.Ledger,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> = ledgerMessenger.sendDerivePublicKeyRequest(
        interactionId = UUIDGenerator.uuid().toString(),
        keyParameters = input.derivationPaths.map { derivationPath ->
            LedgerInteractionRequest.KeyParameters.from(derivationPath)
        },
        ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(factorSource = factorSource)
    ).mapCatching { response ->
        response.publicKeys.map { key ->
            val derivationPath = input.derivationPaths.find {
                it.bip32String == key.derivationPath
            } ?: error("Such derivation path ${key.derivationPath} was not requested")

            HierarchicalDeterministicFactorInstance(
                factorSourceId = factorSource.value.id,
                publicKey = HierarchicalDeterministicPublicKey(
                    publicKey = when (key.curve) {
                        LedgerResponse.DerivedPublicKey.Curve.Curve25519 -> PublicKey.Ed25519.init(hex = key.publicKeyHex)
                        LedgerResponse.DerivedPublicKey.Curve.Secp256k1 -> PublicKey.Secp256k1.init(hex = key.publicKeyHex)
                    },
                    derivationPath = derivationPath
                )
            )
        }
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    override suspend fun signMono(
        factorSource: FactorSource.Ledger,
        input: PerFactorSourceInput<out Signable.Payload, out Signable.ID>
    ): Result<PerFactorOutcome<Signable.ID>> {
        val hdSignatures = input.perTransaction.map { perTransaction ->
            when (val payload = perTransaction.payload) {
                is Signable.Payload.Transaction -> signTransaction(
                    inputPerTransaction = perTransaction,
                    payload = payload,
                    ledgerFactorSource = factorSource
                )

                is Signable.Payload.Subintent -> signSubintent(
                    inputPerTransaction = perTransaction,
                    payload = payload,
                    ledgerFactorSource = factorSource
                )

                is Signable.Payload.Auth -> signAuth(
                    inputPerTransaction = perTransaction,
                    payload = payload,
                    ledgerFactorSource = factorSource
                )
            }.getOrElse { error ->
                return Result.failure(error)
            }
        }.flatten()

        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)

        return Result.success(
            PerFactorOutcome(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcome.Signed(producedSignatures = hdSignatures)
            )
        )
    }

    private suspend fun signTransaction(
        inputPerTransaction: TransactionSignRequestInput<out Signable.Payload>,
        payload: Signable.Payload.Transaction,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<List<HdSignature<Signable.ID>>> {
        val payloadId = inputPerTransaction.payload.getSignable().getId()
        return ledgerMessenger.signTransactionRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            hdPublicKeys = inputPerTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
            compiledTransactionIntent = payload.value.bytes.hex,
            ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
        ).then { response ->
            runCatching {
                response.signatures.map {
                    it.toHDSignature(
                        payloadId = payloadId,
                        ownedFactorInstances = inputPerTransaction.ownedFactorInstances
                    )
                }
            }.mapError {
                RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                    reason = LedgerErrorCode.Generic,
                    message = it.message
                )
            }
        }
    }

    private suspend fun signSubintent(
        inputPerTransaction: TransactionSignRequestInput<out Signable.Payload>,
        payload: Signable.Payload.Subintent,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<List<HdSignature<Signable.ID>>> {
        val payloadId = inputPerTransaction.payload.getSignable().getId()
        return ledgerMessenger.signSubintentHashRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            hdPublicKeys = inputPerTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
            subintentHash = payload.value.decompile().hash().hash.hex,
            ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
        ).then { response ->
            runCatching {
                response.signatures.map {
                    it.toHDSignature(
                        payloadId = payloadId,
                        ownedFactorInstances = inputPerTransaction.ownedFactorInstances
                    )
                }
            }.mapError {
                RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                    reason = LedgerErrorCode.Generic,
                    message = it.message
                )
            }
        }
    }

    private suspend fun signAuth(
        inputPerTransaction: TransactionSignRequestInput<out Signable.Payload>,
        payload: Signable.Payload.Auth,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<List<HdSignature<Signable.ID>>> {
        val payloadId = inputPerTransaction.payload.getSignable().getId()
        return ledgerMessenger.signChallengeRequest(
            interactionId = UUIDGenerator.uuid().toString(),
            hdPublicKeys = inputPerTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
            challengeHex = payload.value.challengeNonce.hex,
            origin = payload.value.origin,
            dAppDefinitionAddress = payload.value.dappDefinitionAddress.string,
            ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
        ).then { response ->
            runCatching {
                response.signatures.map {
                    it.toHDSignature(
                        payloadId = payloadId,
                        ownedFactorInstances = inputPerTransaction.ownedFactorInstances
                    )
                }
            }.mapError {
                RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge
            }
        }
    }

    private fun FactorSource.Ledger.toLedgerDeviceModel() = LedgerInteractionRequest.LedgerDevice(
        name = value.hint.label,
        model = LedgerDeviceModel.from(value.hint.model),
        id = value.id.body.hex
    )

    private fun LedgerResponse.SignatureOfSigner.toHDSignature(
        payloadId: Signable.ID,
        ownedFactorInstances: List<OwnedFactorInstance>
    ): HdSignature<Signable.ID> {
        val ownedFactorInstance = ownedFactorInstances.find {
            it.factorInstance.publicKey.derivationPath.bip32String == derivedPublicKey.derivationPath
        } ?: error("No derivation path from ledger, matched the input ownedFactorInstances.")

        val input = HdSignatureInput(
            payloadId = payloadId,
            ownedFactorInstance = ownedFactorInstance
        )

        return HdSignature(
            input = input,
            signature = toSignatureWithPublicKey()
        )
    }

    private fun LedgerResponse.SignatureOfSigner.toSignatureWithPublicKey(): SignatureWithPublicKey = when (derivedPublicKey.curve) {
        LedgerResponse.DerivedPublicKey.Curve.Curve25519 -> {
            val signature = Signature.Ed25519.init(signature.hexToBagOfBytes())
            val publicKey = PublicKey.Ed25519.init(derivedPublicKey.publicKeyHex)

            SignatureWithPublicKey.init(signature, publicKey)
        }

        LedgerResponse.DerivedPublicKey.Curve.Secp256k1 -> {
            val signature = Signature.Secp256k1.init(signature.hexToBagOfBytes())
            val publicKey = PublicKey.Secp256k1.init(derivedPublicKey.publicKeyHex)

            SignatureWithPublicKey.init(signature, publicKey)
        }
    }
}
