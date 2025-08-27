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
    ): Result<List<HierarchicalDeterministicFactorInstance>> {
        return sargonOsManager.callSafely(dispatcher) {
            arculusCardDerivePublicKeys(factorSource.value, paths)
        }.mapArculusError()
    }

    suspend fun validateMinFirmwareVersion(): Result<Unit> {
        return sargonOsManager.callSafely(dispatcher) {
            arculusCardValidateMinFirmwareVersion()
        }.then { requirement ->
            if (requirement is ArculusMinFirmwareVersionRequirement.Invalid) {
                Result.failure(
                    RadixWalletException.Arculus.MinimumFirmwareRequired(
                        version = requirement.v1
                    )
                )
            } else {
                Result.success(Unit)
            }
        }.mapArculusError()
    }

    suspend fun configureCardWithMnemonic(
        mnemonic: Mnemonic,
        pin: String
    ): Result<Unit> {
        return sargonOsManager.callSafely(dispatcher) {
            arculusCardConfigureWithMnemonic(
                mnemonic = mnemonic,
                pin = pin
            )
            Unit
        }.mapArculusError()
    }

    suspend fun restoreCardPin(
        factorSource: FactorSource.ArculusCard,
        mnemonic: Mnemonic,
        pin: String
    ): Result<Unit> {
        return sargonOsManager.callSafely(dispatcher) {
            arculusCardRestorePin(
                factorSource = factorSource.value,
                mnemonic = mnemonic,
                pin = pin
            )
        }.mapArculusError()
    }

    suspend fun verifyPin(
        factorSource: FactorSource.ArculusCard,
        pin: String
    ): Result<Unit> {
        return sargonOsManager.callSafely(dispatcher) {
            verifyCardPin(
                factorSource = factorSource.value,
                pin = pin
            )
        }.mapArculusError()
    }

    suspend fun setPin(
        factorSource: FactorSource.ArculusCard,
        oldPin: String,
        newPin: String
    ): Result<Unit> {
        return sargonOsManager.callSafely(dispatcher) {
            setCardPin(
                factorSource = factorSource.value,
                oldPin = oldPin,
                newPin = newPin
            )
        }.mapArculusError()
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
        }.mapArculusError()
    }

    private fun <T> Result<T>.mapArculusError(): Result<T> = mapError {
        when (it) {
            is CommonException.ArculusCardWrongPin -> if (it.numberOfRemainingTries > 0) {
                RadixWalletException.Arculus.WrongPin(
                    numberOfTries = it.numberOfRemainingTries.toInt()
                )
            } else {
                RadixWalletException.Arculus.CardBlocked
            }

            is CommonException.NfcSessionLostTagConnection -> RadixWalletException.Arculus.NfcSessionLostTagConnection
            is CommonException.NfcSessionUnknownTag -> RadixWalletException.Arculus.NfcSessionUnknownTag
            else -> it
        }
    }
}
