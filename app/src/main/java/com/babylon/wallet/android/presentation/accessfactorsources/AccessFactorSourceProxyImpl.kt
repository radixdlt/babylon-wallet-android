package com.babylon.wallet.android.presentation.accessfactorsource

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@ActivityRetainedScoped
class AccessFactorSourceProxyImpl @Inject constructor(
    private val appEventBus: AppEventBus
) : AccessFactorSourceProxy, AccessFactorSourceUiProxy {

    private var input: AccessFactorSourceInput = AccessFactorSourceInput.Init
    private val _output = MutableSharedFlow<AccessFactorSourceOutput?>()

    override suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourceInput: AccessFactorSourceInput.ToCreateAccount
    ): AccessFactorSourceOutput.PublicKeyAndDerivationPath {
        input = accessFactorSourceInput
        appEventBus.sendEvent(AppEvent.AccessFactorSource.ToCreateAccount(accessFactorSourceInput.factorSource != null))
        val result = _output.first() ?: throw CancellationException("Authentication dismissed")
        return result as AccessFactorSourceOutput.PublicKeyAndDerivationPath
    }

    override fun getInput(): AccessFactorSourceInput {
        return input
    }

    override suspend fun setOutput(output: AccessFactorSourceOutput?) {
        _output.emit(output)
        reset() // access to factor sources is done
    }

    override suspend fun reset() {
        input = AccessFactorSourceInput.Init
        _output.emit(null)
    }
}
