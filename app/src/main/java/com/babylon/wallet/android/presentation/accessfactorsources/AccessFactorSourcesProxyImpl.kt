package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.MnemonicWithPassphrase
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@ActivityRetainedScoped
class AccessFactorSourcesProxyImpl @Inject constructor(
    private val appEventBus: AppEventBus
) : AccessFactorSourcesProxy, AccessFactorSourcesUiProxy {

    private var input: AccessFactorSourcesInput = AccessFactorSourcesInput.Init
    private val _output = MutableSharedFlow<AccessFactorSourcesOutput>()

    // used only when recovering accounts from onboarding (reDeriveAccounts)
    private var tempMnemonicWithPassphrase: MnemonicWithPassphrase? = null

    override suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKey
    ): Result<AccessFactorSourcesOutput.HDPublicKey> {
        input = accessFactorSourcesInput
        appEventBus.sendEvent(event = AppEvent.AccessFactorSources.DerivePublicKey)
        val result = _output.first()

        return if (result is AccessFactorSourcesOutput.Failure) {
            Result.failure(result.error)
        } else {
            Result.success(result as AccessFactorSourcesOutput.HDPublicKey)
        }
    }

    override suspend fun reDeriveAccounts(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts
    ): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath> {
        input = accessFactorSourcesInput
        tempMnemonicWithPassphrase = null // at this point the DeriveAccountsViewModel has already received the mnemonic

        appEventBus.sendEvent(event = AppEvent.AccessFactorSources.DeriveAccounts)
        val result = _output.first()

        return if (result is AccessFactorSourcesOutput.Failure) {
            Result.failure(result.error)
        } else {
            Result.success(result as AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath)
        }
    }

    override suspend fun createPersona(
        accessFactorSourcesInput: AccessFactorSourcesInput.CreatePersona
    ): Result<AccessFactorSourcesOutput.CreatedPersona> {
        input = accessFactorSourcesInput
        appEventBus.sendEvent(event = AppEvent.AccessFactorSources.CreatePersona)
        val result = _output.first()

        return if (result is AccessFactorSourcesOutput.Failure) {
            Result.failure(result.error)
        } else {
            Result.success(result as AccessFactorSourcesOutput.CreatedPersona)
        }
    }

    override fun getInput(): AccessFactorSourcesInput {
        return input
    }

    override suspend fun setOutput(output: AccessFactorSourcesOutput) {
        _output.emit(output)
        reset() // access to factor sources is done
    }

    private suspend fun reset() {
        input = AccessFactorSourcesInput.Init
        _output.emit(AccessFactorSourcesOutput.Init)
    }

    override fun setTempMnemonicWithPassphrase(mnemonicWithPassphrase: MnemonicWithPassphrase) {
        tempMnemonicWithPassphrase = mnemonicWithPassphrase
    }

    override fun getTempMnemonicWithPassphrase(): MnemonicWithPassphrase? {
        return tempMnemonicWithPassphrase
    }
}
