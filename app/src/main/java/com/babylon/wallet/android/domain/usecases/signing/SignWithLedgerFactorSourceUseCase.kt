package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.LedgerResponse
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HDSignatureInput
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HdSignature
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.InputPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.SignaturesPerFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.first
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapError
import rdx.works.core.sargon.Signable
import rdx.works.core.sargon.init
import rdx.works.core.sargon.updateLastUsed
import rdx.works.core.then
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import javax.inject.Inject

class SignWithLedgerFactorSourceUseCase @Inject constructor(
    private val ledgerMessenger: LedgerMessenger,
    private val profileRepository: ProfileRepository
) {

    /**
     * Guarantees to return a `RadixWalletException.LedgerCommunicationException` in case of an error.
     */
    suspend fun mono(
        ledgerFactorSource: FactorSource.Ledger,
        input: InputPerFactorSource<Signable.Payload>
    ): Result<SignaturesPerFactorSource<Signable.ID>> {
        val hdSignatures = input.transactions.map { perTransaction ->
            val payloadId = perTransaction.payload.getSignable().getId()
            val interactionId = UUIDGenerator.uuid().toString()

            when (val payload = perTransaction.payload) {
                is Signable.Payload.Transaction -> ledgerMessenger.signTransactionRequest(
                    interactionId = interactionId,
                    hdPublicKeys = perTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                    compiledTransactionIntent = payload.value.bytes.hex,
                    ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
                ).then { response ->
                    runCatching {
                        response.signatures.map {
                            it.toHDSignature(
                                payloadId = payloadId,
                                ownedFactorInstances = perTransaction.ownedFactorInstances
                            )
                        }
                    }.mapError {
                        RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                            reason = LedgerErrorCode.Generic,
                            message = it.message
                        )
                    }
                }

                is Signable.Payload.Subintent -> ledgerMessenger.signSubintentHashRequest(
                    interactionId = interactionId,
                    hdPublicKeys = perTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                    subintentHash = payload.value.decompile().hash().hash.hex,
                    ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
                ).then { response ->
                    runCatching {
                        response.signatures.map {
                            it.toHDSignature(
                                payloadId = payloadId,
                                ownedFactorInstances = perTransaction.ownedFactorInstances
                            )
                        }
                    }.mapError {
                        RadixWalletException.LedgerCommunicationException.FailedToSignTransaction(
                            reason = LedgerErrorCode.Generic,
                            message = it.message
                        )
                    }
                }

                is Signable.Payload.Auth -> ledgerMessenger.signChallengeRequest(
                    interactionId = interactionId,
                    hdPublicKeys = perTransaction.ownedFactorInstances.map { it.factorInstance.publicKey },
                    challengeHex = payload.value.challengeNonce.hex,
                    origin = payload.value.origin.toString(),
                    dAppDefinitionAddress = payload.value.dappDefinitionAddress.string,
                    ledgerDevice = ledgerFactorSource.toLedgerDeviceModel()
                ).then { response ->
                    runCatching {
                        response.signatures.map {
                            it.toHDSignature(
                                payloadId = payloadId,
                                ownedFactorInstances = perTransaction.ownedFactorInstances
                            )
                        }
                    }.mapError {
                        RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge
                    }
                }
            }.getOrElse { error ->
                return Result.failure(error)
            }
        }.flatten()

        val profile = profileRepository.profile.first()
        profileRepository.saveProfile(profile.updateLastUsed(ledgerFactorSource.id))

        return Result.success(
            SignaturesPerFactorSource(
                factorSourceId = input.factorSourceId,
                hdSignatures = hdSignatures
            )
        )
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
            it.factorInstance.publicKey.derivationPath.string == derivedPublicKey.derivationPath
        } ?: error("No derivation path from ledger, matched the input ownedFactorInstances.")

        val input = HDSignatureInput(
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
