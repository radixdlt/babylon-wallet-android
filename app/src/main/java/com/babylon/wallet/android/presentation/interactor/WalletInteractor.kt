package com.babylon.wallet.android.presentation.interactor

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.OutputPerFactorSource.Companion.into
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignWithFactorsOutcomeOfAuthIntentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfSubintentHash
import com.radixdlt.sargon.SignWithFactorsOutcomeOfTransactionIntentHash
import kotlinx.coroutines.delay
import rdx.works.core.sargon.Signable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

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
            }.also {
                delay(delayPerFactorSource)
            }
        }

        return KeyDerivationResponse(perFactorSource = publicKeysPerFactorSource)
    }

    override suspend fun signAuth(request: SignRequestOfAuthIntent): SignWithFactorsOutcomeOfAuthIntentHash =
        if (request.factorSourceKind == FactorSourceKind.DEVICE) {
            accessFactorSourcesProxy.sign<Signable.Payload.Auth, Signable.ID.Auth>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromAuthIntents(
                    kind = request.factorSourceKind,
                    input = request.perFactorSource
                )
            ).perFactorSource
        } else {
            request.perFactorSource.map { perFactorSource ->
                accessFactorSourcesProxy.sign<Signable.Payload.Auth, Signable.ID.Auth>(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromAuthIntents(
                        kind = request.factorSourceKind,
                        input = listOf(perFactorSource)
                    )
                ).perFactorSource.first().also {
                    delay(delayPerFactorSource)
                }
            }
        }.into()

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignWithFactorsOutcomeOfSubintentHash =
        if (request.factorSourceKind == FactorSourceKind.DEVICE) {
            accessFactorSourcesProxy.sign<Signable.Payload.Subintent, Signable.ID.Subintent>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromSubintents(
                    kind = request.factorSourceKind,
                    input = request.perFactorSource
                )
            ).perFactorSource
        } else {
            request.perFactorSource.map { perFactorSource ->
                accessFactorSourcesProxy.sign<Signable.Payload.Subintent, Signable.ID.Subintent>(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromSubintents(
                        kind = request.factorSourceKind,
                        input = listOf(perFactorSource)
                    )
                ).perFactorSource.first().also {
                    delay(delayPerFactorSource)
                }
            }
        }.into()

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignWithFactorsOutcomeOfTransactionIntentHash =
        if (request.factorSourceKind == FactorSourceKind.DEVICE) {
            accessFactorSourcesProxy.sign<Signable.Payload.Transaction, Signable.ID.Transaction>(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromTransactionIntents(
                    kind = request.factorSourceKind,
                    input = request.perFactorSource
                )
            ).perFactorSource
        } else {
            request.perFactorSource.map { perFactorSource ->
                accessFactorSourcesProxy.sign<Signable.Payload.Transaction, Signable.ID.Transaction>(
                    accessFactorSourcesInput = AccessFactorSourcesInput.ToSign.fromTransactionIntents(
                        kind = request.factorSourceKind,
                        input = listOf(perFactorSource)
                    )
                ).perFactorSource.first().also {
                    delay(delayPerFactorSource)
                }
            }
        }.into()

    companion object {

        /**
         * The delay between showing a different factor sources bottom sheets.
         */
        private val delayPerFactorSource = 250.milliseconds
    }
}
