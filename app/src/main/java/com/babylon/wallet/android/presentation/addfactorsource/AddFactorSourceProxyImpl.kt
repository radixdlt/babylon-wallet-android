package com.babylon.wallet.android.presentation.addfactorsource

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFactorSourceProxyImpl @Inject constructor(
    private val appEventBus: AppEventBus
) : AddFactorSourceProxy, AddFactorSourceIOHandler {

    private var input: AddFactorSourceInput = AddFactorSourceInput.Init
    private val _output = MutableSharedFlow<AddFactorSourceOutput>()

    private var intermediaryParams: AddFactorSourceIntermediaryParams? = null

    override suspend fun addFactorSource(input: AddFactorSourceInput): AddFactorSourceOutput.Id? {
        this.input = input

        appEventBus.sendEvent(AppEvent.AddFactorSource(input))
        val result = _output.first()

        return result as? AddFactorSourceOutput.Id
    }

    override fun getInput(): AddFactorSourceInput {
        return input
    }

    override suspend fun setOutput(output: AddFactorSourceOutput) {
        _output.emit(output)
        reset()
    }

    override fun setIntermediaryParams(params: AddFactorSourceIntermediaryParams) {
        intermediaryParams = params
    }

    override fun getIntermediaryParams(): AddFactorSourceIntermediaryParams? {
        return intermediaryParams
    }

    private suspend fun reset() {
        input = AddFactorSourceInput.Init
        _output.emit(AddFactorSourceOutput.Init)
    }
}
