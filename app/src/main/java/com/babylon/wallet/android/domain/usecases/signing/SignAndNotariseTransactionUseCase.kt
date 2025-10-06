package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.babylon.wallet.android.domain.usecases.transaction.TransactionConfig
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RoleKind
import com.radixdlt.sargon.SignedIntent
import com.radixdlt.sargon.TransactionGuarantee
import com.radixdlt.sargon.TransactionHeader
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.extensions.summary
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.core.domain.transaction.NotarizationResult
import javax.inject.Inject

class SignAndNotariseTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val resolveSignersUseCase: ResolveSignersUseCase,
    private val sargonOsManager: SargonOsManager
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        manifest: TransactionManifest,
        networkId: NetworkId = manifest.networkId,
        message: Message = Message.None,
        lockFee: Decimal192 = TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192(),
        tipPercentage: UShort = TransactionConfig.TIP_PERCENTAGE,
        notarySecretKey: Curve25519SecretKey = Curve25519SecretKey.secureRandom(),
        feePayerAddress: AccountAddress? = null,
        guarantees: List<TransactionGuarantee> = emptyList()
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
                sargonOsManager.sargonOs.modifyTransactionManifestWithFeePayer(
                    transactionManifest = manifest,
                    feePayerAddress = feePayerAddress,
                    fee = lockFee,
                    guarantees = guarantees
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

    @Suppress("LongParameterList")
    private suspend fun sign(
        networkId: NetworkId,
        manifest: TransactionManifest,
        message: Message,
        notaryKey: PublicKey,
        tipPercentage: UShort,
        epochRange: OpenEndRange<Epoch>,
    ) = resolveSignersUseCase(summary = manifest.summary).mapCatching { signers ->
        val intent = TransactionIntent.from(
            networkId = networkId,
            manifest = manifest,
            message = message,
            notaryPublicKey = notaryKey,
            notaryIsSignatory = signers.isEmpty(),
            tipPercentage = tipPercentage,
            epochs = epochRange
        )

        sargonOsManager.sargonOs.signTransaction(
            transactionIntent = intent,
            roleKind = RoleKind.PRIMARY
        )
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

    @Suppress("LongParameterList")
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
