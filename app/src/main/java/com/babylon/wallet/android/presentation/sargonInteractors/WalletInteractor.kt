package com.babylon.wallet.android.presentation.sargonInteractors

import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
import com.radixdlt.sargon.AuthorizationPurpose
import com.radixdlt.sargon.AuthorizationResponse
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.KeyDerivationRequest
import com.radixdlt.sargon.KeyDerivationResponse
import com.radixdlt.sargon.KeyDerivationResponsePerFactorSource
import com.radixdlt.sargon.SignRequestOfAuthIntent
import com.radixdlt.sargon.SignRequestOfSubintent
import com.radixdlt.sargon.SignRequestOfTransactionIntent
import com.radixdlt.sargon.SignResponseOfAuthIntentHash
import com.radixdlt.sargon.SignResponseOfSubintentHash
import com.radixdlt.sargon.SignResponseOfTransactionIntentHash
import com.radixdlt.sargon.SpotCheckResponse
import com.radixdlt.sargon.os.signing.into
import com.radixdlt.sargon.os.signing.intoSargon
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class WalletInteractor(
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy
) : HostInteractor {

    override suspend fun requestAuthorization(purpose: AuthorizationPurpose): AuthorizationResponse {
        return accessFactorSourcesProxy.requestAuthorization(AccessFactorSourcesInput.ToRequestAuthorization(purpose)).output
    }

    override suspend fun deriveKeys(request: KeyDerivationRequest): KeyDerivationResponse {
        val publicKeysPerFactorSource = request.perFactorSource.map {
            val result = accessFactorSourcesProxy.derivePublicKeys(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKeys(
                    purpose = request.derivationPurpose,
                    request = it
                )
            )

            when (result) {
                is AccessFactorSourcesOutput.DerivedPublicKeys.Success -> {
                    KeyDerivationResponsePerFactorSource(
                        result.factorSourceId,
                        result.factorInstances
                    )
                }

                else -> throw CommonException.HostInteractionAborted()
            }.also {
                delay(delayPerFactorSource)
            }
        }

        if (request.perFactorSource.size == 1) {
            delay(delayPerFactorSource)
        }

        return KeyDerivationResponse(perFactorSource = publicKeysPerFactorSource)
    }

    override suspend fun signAuth(request: SignRequestOfAuthIntent): SignResponseOfAuthIntentHash {
        val perFactorOutcome = request.perFactorSource.mapIndexed { index, input ->
            val output = accessFactorSourcesProxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.AuthIntents,
                    kind = input.factorSourceId.kind,
                    input = input.into()
                )
            )

            val outcome = when (output) {
                is AccessFactorSourcesOutput.SignOutput.Completed -> output.outcome.intoSargon()
                else -> throw CommonException.HostInteractionAborted()
            }

            if (index != request.perFactorSource.lastIndex) {
                delay(delayPerFactorSource)
            }

            outcome
        }

        if (request.perFactorSource.size == 1) {
            delay(delayPerFactorSource)
        }

        return SignResponseOfAuthIntentHash(
            perFactorOutcome = perFactorOutcome
        )
    }

    override suspend fun signSubintents(request: SignRequestOfSubintent): SignResponseOfSubintentHash {
        val perFactorOutcome = request.perFactorSource.mapIndexed { index, input ->
            val output = accessFactorSourcesProxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.SubIntents,
                    kind = input.factorSourceId.kind,
                    input = input.into()
                )
            )

            val outcome = when (output) {
                is AccessFactorSourcesOutput.SignOutput.Completed -> output.outcome.intoSargon()
                else -> throw CommonException.HostInteractionAborted()
            }

            if (index != request.perFactorSource.lastIndex) {
                delay(delayPerFactorSource)
            }

            outcome
        }

        if (request.perFactorSource.size == 1) {
            delay(delayPerFactorSource)
        }

        return SignResponseOfSubintentHash(
            perFactorOutcome = perFactorOutcome
        )
    }

    override suspend fun signTransactions(request: SignRequestOfTransactionIntent): SignResponseOfTransactionIntentHash {
        val perFactorOutcome = request.perFactorSource.mapIndexed { index, input ->
            val output = accessFactorSourcesProxy.sign(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToSign(
                    purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    kind = input.factorSourceId.kind,
                    input = input.into()
                )
            )

            val outcome = when (output) {
                is AccessFactorSourcesOutput.SignOutput.Completed -> output.outcome.intoSargon()
                else -> throw CommonException.HostInteractionAborted()
            }

            if (index != request.perFactorSource.lastIndex) {
                delay(delayPerFactorSource)
            }

            outcome
        }

        if (request.perFactorSource.size == 1) {
            delay(delayPerFactorSource)
        }

        return SignResponseOfTransactionIntentHash(
            perFactorOutcome = perFactorOutcome
        )
    }

    override suspend fun spotCheck(factorSource: FactorSource, allowSkip: Boolean): SpotCheckResponse {
        val output = accessFactorSourcesProxy.spotCheck(
            factorSource = factorSource,
            allowSkip = allowSkip
        )

        return when (output) {
            is AccessFactorSourcesOutput.SpotCheckOutput.Completed -> output.response
            is AccessFactorSourcesOutput.SpotCheckOutput.Rejected -> throw CommonException.HostInteractionAborted()
        }
    }

    companion object {

        /**
         * The delay between showing a different factor sources bottom sheets.
         */
        private val delayPerFactorSource = 250.milliseconds
    }
}
