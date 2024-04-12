package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionConfig.EPOCH_WINDOW
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.modifyLockFee
import com.radixdlt.sargon.extensions.secureRandom
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.then
import rdx.works.core.crypto.PrivateKey
import javax.inject.Inject

class SignTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notariseTransactionUseCase: NotariseTransactionUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase
) {

    // Hopefully this will be removed when this process becomes interactive
    val signingState = collectSignersSignaturesUseCase.interactionState

    // Hopefully this will be removed when this process becomes interactive
    fun cancelSigning() {
        collectSignersSignaturesUseCase.cancel()
    }

    suspend fun sign(
        request: Request,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ): Result<NotarizationResult> {
        val manifestWithLockFee = request.manifestWithLockFee

        val entitiesRequiringAuth = manifestWithLockFee.entitiesRequiringAuth()
        return resolveNotaryAndSignersUseCase(
            accountsAddressesRequiringAuth = entitiesRequiringAuth.accounts,
            personaAddressesRequiringAuth = entitiesRequiringAuth.identities,
            notary = request.ephemeralNotaryPrivateKey
        ).then { notaryAndSigners ->
            transactionRepository.getLedgerEpoch().fold(
                onSuccess = {
                    Result.success(notaryAndSigners to it)
                },
                onFailure = {
                    Result.failure(RadixWalletException.DappRequestException.GetEpoch)
                }
            )
        }.then { notarySignersAndEpoch ->
            notariseTransactionUseCase(
                request = NotariseTransactionUseCase.Request(
                    manifestData = manifestWithLockFee,
                    notaryPublicKey = notarySignersAndEpoch.first.notaryPublicKeyNew(),
                    notaryIsSignatory = notarySignersAndEpoch.first.notaryIsSignatory,
                    startEpoch = notarySignersAndEpoch.second,
                    endEpoch = notarySignersAndEpoch.second + EPOCH_WINDOW,
                    nonce = Nonce.secureRandom(),
                    tipPercentage = request.tipPercentage
                ),
                signatureGatherer = WalletSignatureGatherer(
                    notaryAndSigners = notarySignersAndEpoch.first,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider,
                    collectSignersSignaturesUseCase = collectSignersSignaturesUseCase
                ),
            )
        }
    }

    data class Request(
        private val manifest: TransactionManifestData,
        val lockFee: Decimal192,
        val tipPercentage: UShort,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val feePayerAddress: AccountAddress? = null
    ) {

        val manifestWithLockFee: TransactionManifestData
            get() = if (feePayerAddress == null) {
                manifest
            } else {
                TransactionManifestData.from(
                    manifest = manifest.manifestSargon.modifyLockFee(
                        addressOfFeePayer = feePayerAddress,
                        fee = lockFee
                    ),
                    message = manifest.message
                )
            }
    }

    /**
     * Responsible for signing data
     */
    class WalletSignatureGatherer(
        private val notaryAndSigners: NotaryAndSigners,
        private val deviceBiometricAuthenticationProvider: suspend () -> Boolean,
        private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    ) : NotariseTransactionUseCase.SignatureGatherer {
        override suspend fun gatherSignatures(intent: TransactionIntent): Result<List<SignatureWithPublicKey>> = runCatching {
            SignRequest.SignTransactionRequest(intent = intent)
        }.then { signRequest ->
            collectSignersSignaturesUseCase(
                signers = notaryAndSigners.signers,
                signRequest = signRequest,
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            )
        }

        override suspend fun notarise(signedIntentHash: SignedIntentHash): Result<Signature> = runCatching {
            notaryAndSigners.signWithNotary(signedIntentHash = signedIntentHash)
        }
    }
}
