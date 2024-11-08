package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode.UserRejectedSigningOfTransaction
import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException.RejectedByUser
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.IntentDiscriminator
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.mapError
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.extensions.summary
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class SignSubintentUseCase @Inject constructor(
    private val resolveSignersUseCase: ResolveSignersUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(
        manifest: SubintentManifest,
        message: String?,
        expiration: SubintentExpiration
    ): Result<SignedSubintent> = createSubintent(
        manifest = manifest,
        message = message,
        expiration = expiration
    ).then { subintent ->
        resolveSignersUseCase(summary = manifest.summary).map { subintent to it }
    }.then { subintentWithSigners ->
        val subintent = subintentWithSigners.first
        val signers = subintentWithSigners.second

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

    private suspend fun createSubintent(
        manifest: SubintentManifest,
        message: String?,
        expiration: SubintentExpiration
    ) = runCatching {
        sargonOsManager.sargonOs.createSubintent(
            intentDiscriminator = IntentDiscriminator.random(),
            subintentManifest = manifest,
            expiration = expiration.toDAppInteraction(),
            message = message
        )
    }
}
