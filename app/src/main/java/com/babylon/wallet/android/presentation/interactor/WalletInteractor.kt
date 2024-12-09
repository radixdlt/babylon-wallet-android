package com.babylon.wallet.android.presentation.interactor

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode.UserRejectedSigningOfTransaction
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException.RejectedByUser
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.HdSignatureInputOfSubintentHash
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfSubintentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfSubintentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfTransactionIntentHash
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
import rdx.works.core.sargon.transactionSigningFactorInstance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletInteractor @Inject constructor(
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) : HostInteractor {

    override suspend fun deriveKeys(request: KeyDerivationRequest): KeyDerivationResponse {
        val publicKeysPerFactorSource = request.perFactorSource.map {
            val result = accessFactorSourcesProxy.derivePublicKeys(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKeys(
                    purpose = request.derivationPurpose,
                    factorSourceId = it.factorSourceId,
                    derivationPaths = it.derivationPaths
                )
            )
            when (result) {
                is AccessFactorSourcesOutput.DerivedPublicKeys.Success -> {
                    KeyDerivationResponsePerFactorSource(
                        result.factorSourceId,
                        result.factorInstances
                    )
                }
                is AccessFactorSourcesOutput.DerivedPublicKeys.Failure -> {
                    throw result.error.commonException
                }
            }
        }

        return KeyDerivationResponse(perFactorSource = publicKeysPerFactorSource)
    }

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignWithFactorsOutcomeOfSubintentHash {
        val signaturesPerFactorSource = request.perFactorSource.map { perFactorSource ->
            val hdSignatures = perFactorSource.transactions.map { perTransaction ->
                val output = accessFactorSourcesProxy.getSignatures(
                    accessFactorSourcesInput = perTransaction.toAccessFactorSourcesInput()
                ).getOrElse {
                    throw it.toCommonException()
                }

                output.toHDSignaturesOfSubintentHash(hash = perTransaction.payload.decompile().hash())
            }.flatten()

            SignaturesPerFactorSourceOfSubintentHash(
                factorSourceId = perFactorSource.factorSourceId,
                hdSignatures = hdSignatures
            )
        }

        return SignWithFactorsOutcomeOfSubintentHash.Signed(
            producedSignatures = SignResponseOfSubintentHash(perFactorSource = signaturesPerFactorSource)
        )
    }

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignWithFactorsOutcomeOfTransactionIntentHash {
        val signaturesPerFactorSource = request.perFactorSource.map { perFactorSource ->
            val hdSignatures = perFactorSource.transactions.map { perTransaction ->
                val output = accessFactorSourcesProxy.getSignatures(
                    accessFactorSourcesInput = perTransaction.toAccessFactorSourcesInput()
                ).getOrElse {
                    throw it.toCommonException()
                }

                output.toHDSignaturesOfTransactionIntentHash(hash = perTransaction.payload.decompile().hash())
            }.flatten()

            SignaturesPerFactorSourceOfTransactionIntentHash(
                factorSourceId = perFactorSource.factorSourceId,
                hdSignatures = hdSignatures
            )
        }

        return SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
            producedSignatures = SignResponseOfTransactionIntentHash(perFactorSource = signaturesPerFactorSource)
        )
    }

    private fun TransactionSignRequestInputOfTransactionIntent.toAccessFactorSourcesInput(): AccessFactorSourcesInput.ToGetSignatures =
        AccessFactorSourcesInput.ToGetSignatures(
            signPurpose = SignPurpose.SignTransaction,
            signers = ownedFactorInstances.map { it.owner },
            signRequest = SignRequest.TransactionIntentSignRequest(transactionIntent = payload.decompile())
        )

    private fun AccessFactorSourcesOutput.EntitiesWithSignatures.toHDSignaturesOfTransactionIntentHash(
        hash: TransactionIntentHash
    ): List<HdSignatureOfTransactionIntentHash> =
        signersWithSignatures.map { signerWithSignature ->
            val input = HdSignatureInputOfTransactionIntentHash(
                payloadId = hash,
                ownedFactorInstance = OwnedFactorInstance(
                    owner = signerWithSignature.key.address,
                    factorInstance = signerWithSignature.key.securityState.transactionSigningFactorInstance
                )
            )

            HdSignatureOfTransactionIntentHash(
                input = input,
                signature = signerWithSignature.value
            )
        }

    private fun TransactionSignRequestInputOfSubintent.toAccessFactorSourcesInput(): AccessFactorSourcesInput.ToGetSignatures =
        AccessFactorSourcesInput.ToGetSignatures(
            signPurpose = SignPurpose.SignTransaction,
            signers = ownedFactorInstances.map { it.owner },
            signRequest = SignRequest.SubintentSignRequest(subintent = payload.decompile())
        )

    private fun AccessFactorSourcesOutput.EntitiesWithSignatures.toHDSignaturesOfSubintentHash(
        hash: SubintentHash
    ): List<HdSignatureOfSubintentHash> =
        signersWithSignatures.map { signerWithSignature ->
            val input = HdSignatureInputOfSubintentHash(
                payloadId = hash,
                ownedFactorInstance = OwnedFactorInstance(
                    owner = signerWithSignature.key.address,
                    factorInstance = signerWithSignature.key.securityState.transactionSigningFactorInstance
                )
            )

            HdSignatureOfSubintentHash(
                input = input,
                signature = signerWithSignature.value
            )
        }

    // TODO check that mapping
    private fun Throwable.toCommonException() =
        // - Derive keys
        // -- CommonException.SecureStorageAccess
        // -- CommonException.SecureStorageReadException
        // -- CommonException.SigningRejected
        //
        // - Signing
        // -- NoMnemonic => CommonException.SecureStorageAccess
        // -- SecureStorageAccess => CommonException.SecureStorageReadException
        // - NONFATAL
        // -- FailedToConnect
        // -- FailedToGetDeviceId
        // -- FailedToDerivePublicKeys
        // -- FailedToDeriveAndDisplayAddress
        // -- FailedToSignAuthChallenge with BlindSigningNotEnabledButRequired or UserRejectedSigningOfTransaction
        if (this is RejectedByUser || (this is FailedToSignTransaction && this.reason == UserRejectedSigningOfTransaction)) {
            CommonException.SigningRejected()
        } else if (this is CommonException) {
            this
        } else {

            CommonException.Unknown()
        }
}