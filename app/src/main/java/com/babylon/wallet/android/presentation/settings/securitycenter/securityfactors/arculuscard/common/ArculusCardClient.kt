package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.signedAuth
import com.babylon.wallet.android.presentation.accessfactorsources.signedSubintent
import com.babylon.wallet.android.presentation.accessfactorsources.signedTransaction
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.ArculusMinFirmwareVersionRequirement
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import rdx.works.core.mapError
import rdx.works.core.then
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArculusCardClient @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun derivePublicKeys(
        factorSource: FactorSource.ArculusCard,
        paths: List<DerivationPath>
    ): Result<List<HierarchicalDeterministicFactorInstance>> = sargonOsManager.callSafely(dispatcher) {
        arculusCardDerivePublicKeys(factorSource.value, paths)
    }

    suspend fun validateMinFirmwareVersion(): Result<Unit> = sargonOsManager.callSafely(dispatcher) {
        arculusCardValidateMinFirmwareVersion()
    }.then { requirement ->
        if (requirement is ArculusMinFirmwareVersionRequirement.Invalid) {
            Result.failure(
                RadixWalletException.FactorSource.ArculusMinimumFirmwareRequired(
                    version = requirement.v1
                )
            )
        } else {
            Result.success(Unit)
        }
    }

    suspend fun configureCardWithMnemonic(
        mnemonic: Mnemonic,
        pin: String
    ): Result<Unit> = sargonOsManager.callSafely(dispatcher) {
        arculusCardConfigureWithMnemonic(
            mnemonic = mnemonic,
            pin = pin
        )
    }

    suspend fun verifyPin(
        factorSource: FactorSource.ArculusCard,
        pin: String
    ): Result<Unit> = sargonOsManager.callSafely(dispatcher) {
        verifyCardPin(
            factorSource = factorSource.value,
            pin = pin
        )
    }.mapError {
        if (it is CommonException.ArculusCardWrongPin) {
            RadixWalletException.FactorSource.ArculusWrongPin(
                numberOfTries = it.numberOfRemainingTries.toInt()
            )
        } else {
            it
        }
    }

    suspend fun setPin(
        factorSource: FactorSource.ArculusCard,
        oldPin: String,
        newPin: String
    ): Result<Unit> = sargonOsManager.callSafely(dispatcher) {
        setCardPin(
            factorSource = factorSource.value,
            oldPin = oldPin,
            newPin = newPin
        )
    }

    suspend fun sign(
        factorSource: FactorSource.ArculusCard,
        input: AccessFactorSourcesInput.Sign,
        pin: String
    ): Result<AccessFactorSourcesOutput.Sign> {
        return sargonOsManager.callSafely(dispatcher) {
            when (input) {
                is AccessFactorSourcesInput.SignAuth -> {
                    AccessFactorSourcesOutput.Sign.signedAuth(
                        factorSourceId = input.factorSourceId,
                        signatures = arculusCardSignAuth(
                            factorSource.value,
                            pin,
                            input.input.perTransaction
                        )
                    )
                }

                is AccessFactorSourcesInput.SignSubintent -> {
                    AccessFactorSourcesOutput.Sign.signedSubintent(
                        factorSourceId = input.factorSourceId,
                        signatures = arculusCardSignSubintent(
                            factorSource.value,
                            pin,
                            input.input.perTransaction
                        )
                    )
                }

                is AccessFactorSourcesInput.SignTransaction -> {
                    AccessFactorSourcesOutput.Sign.signedTransaction(
                        factorSourceId = input.factorSourceId,
                        signatures = arculusCardSignTransaction(
                            factorSource.value,
                            pin,
                            input.input.perTransaction
                        )
                    )
                }
            }
        }
    }
}
