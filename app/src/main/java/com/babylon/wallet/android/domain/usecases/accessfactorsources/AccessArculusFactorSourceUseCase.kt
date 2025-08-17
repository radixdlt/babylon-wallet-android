package com.babylon.wallet.android.domain.usecases.accessfactorsources

import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.FactorOutcomeOfSubintentHash
import com.radixdlt.sargon.FactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import javax.inject.Inject

class AccessArculusFactorSourceUseCase @Inject constructor(
    private val arculusCardClient: ArculusCardClient
) : AccessFactorSource<FactorSource.ArculusCard> {

    override suspend fun derivePublicKeys(
        factorSource: FactorSource.ArculusCard,
        input: KeyDerivationRequestPerFactorSource
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        return arculusCardClient.derivePublicKeys(factorSource, input.derivationPaths)
    }

    override suspend fun signMono(
        factorSource: FactorSource.ArculusCard,
        input: AccessFactorSourcesInput.Sign
    ): Result<AccessFactorSourcesOutput.Signing> {
        when (input) {
            is AccessFactorSourcesInput.SignAuth -> {
                return sargonOsManager.callSafely(dispatcher) {
                    AccessFactorSourcesOutput.SignAuth(
                        PerFactorOutcomeOfAuthIntentHash(
                            factorSourceId = input.factorSourceId,
                            outcome = FactorOutcomeOfAuthIntentHash.Signed(
                                producedSignatures = arculusCardSignAuth(
                                    factorSource.value,
                                    "123456",
                                    input.input.perTransaction
                                )
                            )
                        )
                    )
                }
            }

            is AccessFactorSourcesInput.SignSubintent -> {
                return sargonOsManager.callSafely(dispatcher) {
                    AccessFactorSourcesOutput.SignSubintent(
                        PerFactorOutcomeOfSubintentHash(
                            factorSourceId = input.factorSourceId,
                            outcome = FactorOutcomeOfSubintentHash.Signed(
                                producedSignatures = arculusCardSignSubintent(
                                    factorSource.value,
                                    "123456",
                                    input.input.perTransaction
                                )
                            )
                        )
                    )
                }
            }

            is AccessFactorSourcesInput.SignTransaction -> {
                return sargonOsManager.callSafely(dispatcher) {
                    AccessFactorSourcesOutput.SignTransaction(
                        PerFactorOutcomeOfTransactionIntentHash(
                            factorSourceId = input.factorSourceId,
                            outcome = FactorOutcomeOfTransactionIntentHash.Signed(
                                producedSignatures = arculusCardSignTransaction(
                                    factorSource.value,
                                    "123456",
                                    input.input.perTransaction
                                )
                            )
                        )
                    )
                }
            }
        }
    }
    override suspend fun spotCheck(factorSource: FactorSource.ArculusCard): Result<Boolean> {
        TODO("Future implementation")
    }
}
