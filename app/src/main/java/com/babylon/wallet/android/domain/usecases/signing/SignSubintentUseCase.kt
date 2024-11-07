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
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.Instant
import com.radixdlt.sargon.IntentDiscriminator
import com.radixdlt.sargon.IntentHeaderV2
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.MessageV2
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.extensions.summary
import com.radixdlt.sargon.extensions.then
import javax.inject.Inject

class SignSubintentUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val resolveSignersUseCase: ResolveSignersUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
) {

    suspend operator fun invoke(
        networkId: NetworkId,
        manifest: SubintentManifest,
        message: MessageV2,
        maxProposerTimestamp: Timestamp
    ): Result<SignedSubintent> = resolveSignersUseCase(summary = manifest.summary).then { signers ->
        // TODO will be moved to sargon
        val epochRange = transactionRepository.getLedgerEpoch().getOrElse {
            return Result.failure(RadixWalletException.DappRequestException.GetEpoch)
        }.let { epoch ->
            epoch ..< epoch + TransactionConfig.EPOCH_WINDOW
        }
        val subintent = Subintent.from(
            networkId = networkId,
            manifest = manifest,
            message = message,
            epochs = epochRange,
            maxProposerTimestamp = maxProposerTimestamp
        )

        accessFactorSourcesProxy.getSignatures(
            accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                signPurpose = SignPurpose.SignTransaction,
                signers = signers,
                signRequest = SignRequest.SubintentSignRequest(subintent = subintent)
            )
        ).map { signaturesResult ->
            val intentSignatures = signaturesResult.signersWithSignatures.values.map { IntentSignature.init(it) }
            SignedSubintent(
                subintent = subintent,
                subintentSignatures = IntentSignatures(signatures = intentSignatures)
            )
        }.mapError { error ->
            if (error is RejectedByUser || (error is FailedToSignTransaction && error.reason == UserRejectedSigningOfTransaction)) {
                RejectedByUser
            } else {
                PrepareTransactionException.SignCompiledTransactionIntent(error)
            }
        }
    }

    private fun Subintent.Companion.from(
        networkId: NetworkId,
        manifest: SubintentManifest,
        message: MessageV2,
        epochs: OpenEndRange<Epoch>,
        maxProposerTimestamp: Timestamp
    ): Subintent = Subintent(
        header = IntentHeaderV2(
            networkId = networkId,
            startEpochInclusive = epochs.start,
            endEpochExclusive = epochs.endExclusive,
            minProposerTimestampInclusive = null,
            maxProposerTimestampExclusive = Instant(secondsSinceUnixEpoch = maxProposerTimestamp.toEpochSecond()),
            intentDiscriminator = IntentDiscriminator.random()
        ),
        manifest = manifest,
        message = message
    )

}