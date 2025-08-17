package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.payloadId
import com.radixdlt.sargon.FactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.FactorOutcomeOfSubintentHash
import com.radixdlt.sargon.FactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HdSignatureInputOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfAuthIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.PerFactorSourceInputOfAuthIntent
import com.radixdlt.sargon.PerFactorSourceInputOfSubintent
import com.radixdlt.sargon.PerFactorSourceInputOfTransactionIntent
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SpotCheckInput
import com.radixdlt.sargon.TransactionSignRequestInputOfAuthIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.extensions.bip32String
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.spotCheck
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
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Signing> {
        return when (input) {
            is AccessFactorSourcesInput.SignTransaction -> signTransactionN(
                input = input.input,
                ledgerFactorSource = factorSource
            )
            is AccessFactorSourcesInput.SignSubintent -> signSubintent(
                input = input.input,
                ledgerFactorSource = factorSource
            )
            is AccessFactorSourcesInput.SignAuth -> signAuth(
                input = input.input,
                ledgerFactorSource = factorSource
            )
        }
    }

    override suspend fun spotCheck(factorSource: FactorSource.Ledger): Result<Boolean> = ledgerMessenger.sendDeviceInfoRequest(
        interactionId = UUIDGenerator.uuid().toString()
    ).mapCatching { deviceIdResponse ->
        factorSource.spotCheck(input = SpotCheckInput.Ledger(id = deviceIdResponse.deviceId))
    }.onSuccess {
        updateFactorSourceLastUsedUseCase(factorSourceId = factorSource.id)
    }

    private suspend fun signTransactionN(
        input: PerFactorSourceInputOfTransactionIntent,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<AccessFactorSourcesOutput.SignTransaction> {
        val signatures = input.perTransaction.map { transaction ->
            val payload = transaction.payload
            val payloadId = transaction.payloadId()

            ledgerMessenger.signTransactionRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = transaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                compiledTransactionIntent = payload.bytes.hex,
                ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
            ).then { response ->
                runCatching {
                    response.signatures.map { signature ->
                        val ownedFactorInstance = transaction.ownedFactorInstances.find {
                            it.factorInstance.publicKey.derivationPath.bip32String == signature.derivedPublicKey.derivationPath
                        }
                            ?: error("No derivation path from ledger, matched the input ownedFactorInstances.")

                        HdSignatureOfTransactionIntentHash(
                            input = HdSignatureInputOfTransactionIntentHash(
                                payloadId = payloadId,
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature.toSignatureWithPublicKey()
                        )
                    }
                }.mapError {
                    RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                        reason = LedgerErrorCode.Generic,
                        message = it.message
                    )
                }
            }.getOrElse { error ->
                return Result.failure(error)
            }
        }.flatten()

        return Result.success(
            AccessFactorSourcesOutput.SignTransaction(
            PerFactorOutcomeOfTransactionIntentHash(
                factorSourceId = input.factorSourceId,
                outcome = FactorOutcomeOfTransactionIntentHash.Signed(
                    producedSignatures = signatures
                )
            )
            )
        )
    }

    private suspend fun signSubintent(
        input: PerFactorSourceInputOfSubintent,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<AccessFactorSourcesOutput.SignSubintent> {
        val signatures = input.perTransaction.map { transaction ->
            val payload = transaction.payload
            val payloadId = transaction.payloadId()

            ledgerMessenger.signSubintentHashRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = transaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                subintentHash = payloadId.hash.hex,
                ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
            ).then { response ->
                runCatching {
                    response.signatures.map { signature ->
                        val ownedFactorInstance = transaction.ownedFactorInstances.find {
                            it.factorInstance.publicKey.derivationPath.bip32String == signature.derivedPublicKey.derivationPath
                        }
                            ?: error("No derivation path from ledger, matched the input ownedFactorInstances.")

                        HdSignatureOfSubintentHash(
                            input = HdSignatureInputOfSubintentHash(
                                payloadId = payloadId,
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature.toSignatureWithPublicKey()
                        )
                    }
                }.mapError {
                    RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                        reason = LedgerErrorCode.Generic,
                        message = it.message
                    )

                }
            }.getOrElse { error ->
                return Result.failure(error)
            }
        }.flatten()

        return Result.success(
            AccessFactorSourcesOutput.SignSubintent(
            PerFactorOutcomeOfSubintentHash(
            factorSourceId = input.factorSourceId,
            outcome = FactorOutcomeOfSubintentHash.Signed(
                producedSignatures = signatures
            )
        )
            )
        )
    }

    private suspend fun signAuth(
        input: PerFactorSourceInputOfAuthIntent,
        ledgerFactorSource: FactorSource.Ledger,
    ): Result<AccessFactorSourcesOutput.SignAuth> {
        val signatures = input.perTransaction.map { transaction ->
            val payload = transaction.payload
            val payloadId = transaction.payloadId()

            ledgerMessenger.signChallengeRequest(
                interactionId = UUIDGenerator.uuid().toString(),
                hdPublicKeys = transaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                challengeHex = payload.challengeNonce.hex,
                origin = payload.origin,
                dAppDefinitionAddress = payload.dappDefinitionAddress.string,
                ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
            ).then { response ->
                runCatching {
                    response.signatures.map { signature ->
                        val ownedFactorInstance = transaction.ownedFactorInstances.find {
                            it.factorInstance.publicKey.derivationPath.bip32String == signature.derivedPublicKey.derivationPath
                        }
                            ?: error("No derivation path from ledger, matched the input ownedFactorInstances.")

                        HdSignatureOfAuthIntentHash(
                            input = HdSignatureInputOfAuthIntentHash(
                                payloadId = payloadId,
                                ownedFactorInstance = ownedFactorInstance
                            ),
                            signature = signature.toSignatureWithPublicKey()
                        )
                    }
                }.mapError {
                    RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                        reason = LedgerErrorCode.Generic,
                        message = it.message
                    )

                }
            }.getOrElse { error ->
                return Result.failure(error)
            }
        }.flatten()

        return Result.success(
            AccessFactorSourcesOutput.SignAuth(
            PerFactorOutcomeOfAuthIntentHash(
            factorSourceId = input.factorSourceId,
            outcome = FactorOutcomeOfAuthIntentHash.Signed(
                producedSignatures = signatures
            )
        )
            )
        )
    }

    private fun FactorSource.Ledger.toLedgerDeviceModel() = LedgerInteractionRequest.LedgerDevice(
        name = value.hint.label,
        model = LedgerDeviceModel.from(value.hint.model),
        id = value.id.body.hex
    )

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
