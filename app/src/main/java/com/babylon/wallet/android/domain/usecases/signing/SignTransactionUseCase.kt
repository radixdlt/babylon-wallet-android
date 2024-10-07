package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig.EPOCH_WINDOW
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarySignature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.modifyLockFee
import com.radixdlt.sargon.extensions.secureRandom
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.then
import javax.inject.Inject

class SignTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val notariseTransactionUseCase: NotariseTransactionUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) {

    suspend operator fun invoke(request: Request): Result<NotarizationResult> {
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
                    accessFactorSourcesProxy = accessFactorSourcesProxy
                )
            )
        }
    }

    data class Request(
        private val manifest: TransactionManifestData,
        val lockFee: Decimal192,
        val tipPercentage: UShort,
        val ephemeralNotaryPrivateKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
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
        private val accessFactorSourcesProxy: AccessFactorSourcesProxy
    ) : NotariseTransactionUseCase.SignatureGatherer {
        override suspend fun gatherSignatures(intent: TransactionIntent): Result<List<SignatureWithPublicKey>> = runCatching {
            SignRequest.SignTransactionRequest(intent = intent)
        }.then { signRequest ->
            if (notaryAndSigners.notaryIsSignatory) {
                Result.success(emptyList())
            } else {
                accessFactorSourcesProxy.getSignatures(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                        signPurpose = SignPurpose.SignTransaction,
                        signers = notaryAndSigners.signers,
                        signRequest = signRequest
                    )
                ).mapCatching { result ->
                    result.signersWithSignatures.values.toList()
                }
            }
        }

        override suspend fun notarise(signedIntentHash: SignedIntentHash): Result<NotarySignature> = runCatching {
            notaryAndSigners.signWithNotary(signedIntentHash = signedIntentHash)
        }
    }
}
