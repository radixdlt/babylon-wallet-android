package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionConfig.EPOCH_WINDOW
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignRequest
import rdx.works.core.NonceGenerator
import rdx.works.core.mapError
import rdx.works.core.then
import rdx.works.profile.ret.addLockFee
import rdx.works.profile.ret.crypto.PrivateKey
import rdx.works.profile.ret.crypto.Signature
import rdx.works.profile.ret.crypto.SignatureWithPublicKey
import rdx.works.profile.ret.transaction.TransactionManifestData
import rdx.works.profile.ret.transaction.TransactionSigner
import java.math.BigDecimal
import javax.inject.Inject

class SignTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionSigner: TransactionSigner,
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
    ): Result<TransactionSigner.Notarization> {
        val manifestWithLockFee = request.manifestWithLockFee

        val entitiesRequiringAuth = manifestWithLockFee.entitiesRequiringAuth()
        return resolveNotaryAndSignersUseCase(
            accountsRequiringAuth = entitiesRequiringAuth.accounts,
            personasRequiringAuth = entitiesRequiringAuth.identities,
            notary = request.ephemeralNotaryPrivateKey
        ).then { notaryAndSigners ->
            transactionRepository.getLedgerEpoch().fold(
                onSuccess = {
                    Result.success(notaryAndSigners to it.toULong())
                },
                onFailure = {
                    Result.failure(RadixWalletException.DappRequestException.GetEpoch)
                }
            )
        }.then { notarySignersAndEpoch ->
            transactionSigner.notarise(
                request = TransactionSigner.Request(
                    manifestData = manifestWithLockFee,
                    notaryPublicKey = notarySignersAndEpoch.first.notaryPublicKey(),
                    notaryIsSignatory = notarySignersAndEpoch.first.notaryIsSignatory,
                    startEpoch = notarySignersAndEpoch.second,
                    endEpoch = notarySignersAndEpoch.second + EPOCH_WINDOW,
                    nonce = NonceGenerator(),
                    tipPercentage = request.tipPercentage
                ),
                signatureGatherer = WalletSignatureGatherer(
                    notaryAndSigners = notarySignersAndEpoch.first,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider,
                    collectSignersSignaturesUseCase = collectSignersSignaturesUseCase
                ),
            ).mapError { error ->
                when (error) {
                    is TransactionSigner.Error -> {
                        if (error.cause is RadixWalletException) {
                            return@mapError error.cause as RadixWalletException
                        }

                        when (error) {
                            is TransactionSigner.Error.Prepare -> PrepareTransactionException.PrepareNotarizedTransaction(error.cause)
                            is TransactionSigner.Error.Sign -> PrepareTransactionException.SignCompiledTransactionIntent(error.cause)
                        }
                    }
                    else -> error
                }
            }
        }
    }

    data class Request(
        private val manifest: TransactionManifestData,
        val lockFee: BigDecimal,
        val tipPercentage: UShort,
        val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
        val feePayerAddress: String? = null
    ) {

        val manifestWithLockFee: TransactionManifestData = if (feePayerAddress == null) {
            manifest
        } else {
            manifest.addLockFee(feePayerAddress, lockFee)
        }
    }

    /**
     * Responsible for signing data
     */
    class WalletSignatureGatherer(
        private val notaryAndSigners: NotaryAndSigners,
        private val deviceBiometricAuthenticationProvider: suspend () -> Boolean,
        private val collectSignersSignaturesUseCase: CollectSignersSignaturesUseCase,
    ) : TransactionSigner.SignatureGatherer {
        override suspend fun gatherSignatures(dataToSign: ByteArray, hashedDataToSign: ByteArray): Result<List<SignatureWithPublicKey>> {
            return collectSignersSignaturesUseCase(
                signers = notaryAndSigners.signers,
                signRequest = SignRequest.SignTransactionRequest(
                    dataToSign = dataToSign,
                    hashedDataToSign = hashedDataToSign
                ),
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            )
        }

        override suspend fun notarise(signedIntentHash: ByteArray): Result<Signature> = runCatching {
            notaryAndSigners.signWithNotary(hashedData = signedIntentHash)
        }
    }
}
