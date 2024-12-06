package com.babylon.wallet.android.presentation.interactor

import android.util.Log
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode.UserRejectedSigningOfTransaction
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException.RejectedByUser
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.model.signing.SignPurpose
import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.HdSignatureInputOfTransactionIntentHash
import com.radixdlt.sargon.HdSignatureOfTransactionIntentHash
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.SignaturesPerFactorSourceOfTransactionIntentHash
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.mapError
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.transactionSigningFactorInstance
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletInteractor @Inject constructor(
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
): HostInteractor {

    private var getProfileUseCase: GetProfileUseCase? = null

    fun register(
        getProfileUseCase: GetProfileUseCase
    ) {
        this.getProfileUseCase = getProfileUseCase
    }

    override suspend fun deriveKeys(request: KeyDerivationRequest): KeyDerivationResponse {
        throw CommonException.Unknown()
    }

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignWithFactorsOutcomeOfSubintentHash {
        throw CommonException.SigningRejected()
    }

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignWithFactorsOutcomeOfTransactionIntentHash {
        val profile = getProfileUseCase?.invoke() ?: throw CommonException.SigningRejected()

        val signaturesPerFactorSource = request.perFactorSource.map { perFactorSource ->
            val hdSignatures = perFactorSource.transactions.map { perTransaction ->
                val signRequest = perFactorSource.transactions.first()

                val accounts = profile.activeAccountsOnCurrentNetwork
                val personas = profile.activePersonasOnCurrentNetwork
                val signers = signRequest.ownedFactorInstances.map { owned ->
                    when (val owner = owned.owner) {
                        is AddressOfAccountOrPersona.Account -> accounts.find {
                            it.address == owner.v1
                        }?.asProfileEntity() ?: throw CommonException.UnknownAccount()
                        is AddressOfAccountOrPersona.Identity -> personas.find {
                            it.address == owner.v1
                        }?.asProfileEntity() ?: throw CommonException.UnknownPersona()
                    }
                }

                val entitiesWithSignatures = accessFactorSourcesProxy.getSignatures(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToGetSignatures(
                        signPurpose = SignPurpose.SignTransaction,
                        signers = signers,
                        signRequest = SignRequest.TransactionIntentSignRequest(
                            transactionIntent = signRequest.payload.decompile()
                        )
                    )
                ).mapError { error ->
                    throw if (error is RejectedByUser || (error is FailedToSignTransaction && error.reason == UserRejectedSigningOfTransaction)) {
                        CommonException.SigningRejected()
                    } else {
                        CommonException.Unknown()
                    }
                }.getOrThrow()

                entitiesWithSignatures.signersWithSignatures.map { signerWithSignature ->
                    val input = HdSignatureInputOfTransactionIntentHash(
                        payloadId = perTransaction.payload.decompile().hash(),
                        ownedFactorInstance = OwnedFactorInstance(
                            owner = signerWithSignature.key.address,
                            factorInstance = signerWithSignature.key.securityState.transactionSigningFactorInstance // TODO check that
                        )
                    )

                    HdSignatureOfTransactionIntentHash(
                        input = input,
                        signature = signerWithSignature.value
                    )
                }
            }.flatten()

            SignaturesPerFactorSourceOfTransactionIntentHash(
                factorSourceId = perFactorSource.factorSourceId,
                hdSignatures = hdSignatures
            )
        }

        return SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
            producedSignatures = SignResponseOfTransactionIntentHash(
                perFactorSource = signaturesPerFactorSource
            )
        )
    }

}