package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode.UserRejectedSigningOfTransaction
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException.RejectedByUser
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SignedIntent
import com.radixdlt.sargon.TransactionHeader
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.modifyLockFee
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.extensions.summary
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.transaction.NotarizationResult
import javax.inject.Inject

class SignAndNotariseTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val resolveSignersUseCase: ResolveSignersUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
) {

    suspend operator fun invoke(
        manifest: TransactionManifest,
        networkId: NetworkId = manifest.networkId,
        message: Message = Message.None,
        lockFee: Decimal192 = TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192(),
        tipPercentage: UShort = TransactionConfig.TIP_PERCENTAGE,
        notarySecretKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
        feePayerAddress: AccountAddress? = null
    ): Result<NotarizationResult> {
        val epochRange = transactionRepository.getLedgerEpoch().getOrElse {
            return Result.failure(RadixWalletException.DappRequestException.GetEpoch)
        }.let { epoch ->
            epoch..<epoch + TransactionConfig.EPOCH_WINDOW
        }

        return sign(
            networkId = networkId,
            manifest = if (feePayerAddress == null) {
                manifest
            } else {
                manifest.modifyLockFee(
                    addressOfFeePayer = feePayerAddress,
                    fee = lockFee
                )
            },
            message = message,
            notaryKey = notarySecretKey.toPublicKey(),
            tipPercentage = tipPercentage,
            epochRange = epochRange
        ).then { signedIntent ->
            notarize(
                signedIntent = signedIntent,
                notarySecretKey = notarySecretKey,
                endEpoch = epochRange.endExclusive
            )
        }
    }

    private suspend fun sign(
        networkId: NetworkId,
        manifest: TransactionManifest,
        message: Message,
        notaryKey: PublicKey,
        tipPercentage: UShort,
        epochRange: OpenEndRange<Epoch>,
    ) = resolveSignersUseCase(summary = manifest.summary)
        .then { signers ->
            val intent = TransactionIntent.from(
                networkId = networkId,
                manifest = manifest,
                message = message,
                notaryPublicKey = notaryKey,
                notaryIsSignatory = signers.isEmpty(),
                tipPercentage = tipPercentage,
                epochs = epochRange
            )

            accessFactorSourcesProxy.getSignatures(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                    signPurpose = SignPurpose.SignTransaction,
                    signers = signers,
                    signRequest = SignRequest.TransactionIntentSignRequest(transactionIntent = intent)
                )
            ).map { signaturesResult ->
                val intentSignatures = signaturesResult.signersWithSignatures.values.map { IntentSignature.init(it) }
                SignedIntent(
                    intent = intent,
                    intentSignatures = IntentSignatures(signatures = intentSignatures)
                )
            }
        }.mapError { error ->
            if (error is RejectedByUser || (error is FailedToSignTransaction && error.reason == UserRejectedSigningOfTransaction)) {
                RejectedByUser
            } else {
                PrepareTransactionException.SignCompiledTransactionIntent(error)
            }
        }

    private fun notarize(
        signedIntent: SignedIntent,
        notarySecretKey: Curve25519SecretKey,
        endEpoch: Epoch
    ): Result<NotarizationResult> = runCatching {
        notarySecretKey.notarize(signedTransactionIntentHash = signedIntent.hash())
    }.map { notarySignature ->
        NotarizationResult(
            endEpoch = endEpoch,
            notarizedTransaction = NotarizedTransaction(
                signedIntent = signedIntent,
                notarySignature = notarySignature
            )
        )
    }.mapError { error ->
        PrepareTransactionException.PrepareNotarizedTransaction(error)
    }

    private fun TransactionIntent.Companion.from(
        networkId: NetworkId,
        manifest: TransactionManifest,
        message: Message,
        notaryPublicKey: PublicKey,
        notaryIsSignatory: Boolean,
        tipPercentage: UShort,
        epochs: OpenEndRange<Epoch>,
    ): TransactionIntent = TransactionIntent(
        header = TransactionHeader(
            networkId = networkId,
            startEpochInclusive = epochs.start,
            endEpochExclusive = epochs.endExclusive,
            nonce = Nonce.random(),
            notaryPublicKey = notaryPublicKey,
            notaryIsSignatory = notaryIsSignatory,
            tipPercentage = tipPercentage
        ),
        manifest = manifest,
        message = message
    )
}
