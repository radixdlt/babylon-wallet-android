package com.babylon.wallet.android.presentation.interactor

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfAuthIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
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

    override suspend fun signAuth(request: SignRequestOfAuthIntent): SignWithFactorsOutcomeOfAuthIntentHash {
        throw CommonException.SigningRejected()
    }

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignWithFactorsOutcomeOfSubintentHash {
        throw CommonException.SigningRejected()
    }

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignWithFactorsOutcomeOfTransactionIntentHash {
        return if (request.factorSourceKind == FactorSourceKind.DEVICE) {
            val result = accessFactorSourcesProxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.Transactions(perFactorSource = request.perFactorSource)
            )

            when (result) {
                is AccessFactorSourcesOutput.SignOutput.Success -> {
                    SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
                        producedSignatures = SignResponseOfTransactionIntentHash(
                            perFactorSource = result.perFactorSource
                        )
                    )
                }
                is AccessFactorSourcesOutput.SignOutput.Failure -> {
                    // TODO check that
                    throw result.error.commonException
                }
            }
        } else {
            val perFactorSource = request.perFactorSource.map { perFactorSource ->
                val result = accessFactorSourcesProxy.sign(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.Transactions(listOf(perFactorSource))
                )

                when (result) {
                    is AccessFactorSourcesOutput.SignOutput.Success -> result.perFactorSource.first()
                    is AccessFactorSourcesOutput.SignOutput.Failure -> {
                        // TODO check that
                        throw result.error.commonException
                    }
                }
            }

            SignWithFactorsOutcomeOfTransactionIntentHash.Signed(
                producedSignatures = SignResponseOfTransactionIntentHash(
                    perFactorSource = perFactorSource
                )
            )
        }
    }
}
