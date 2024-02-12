package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
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

    override suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDeriveAccountPublicKey
    ): Result<AccessFactorSourcesOutput.PublicKeyAndDerivationPath> {
        input = accessFactorSourcesInput
        appEventBus.sendEvent(event = AppEvent.AccessFactorSources.DeriveAccountPublicKey)
        val result = _output.first()

        return if (result is AccessFactorSourcesOutput.Failure) {
            Result.failure(result.error)
        } else {
            Result.success(result as AccessFactorSourcesOutput.PublicKeyAndDerivationPath)
        }
    }

    override fun getInput(): AccessFactorSourcesInput {
        return input
    }

    override suspend fun setOutput(output: AccessFactorSourcesOutput) {
        _output.emit(output)
        reset() // access to factor sources is done
    }

    override suspend fun reset() {
        input = AccessFactorSourcesInput.Init
        _output.emit(AccessFactorSourcesOutput.Init)
    }
}
