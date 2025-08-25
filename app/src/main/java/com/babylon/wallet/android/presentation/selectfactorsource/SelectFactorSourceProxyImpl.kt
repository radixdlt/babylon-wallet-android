package com.babylon.wallet.android.presentation.selectfactorsource

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectFactorSourceProxyImpl @Inject constructor(
    private val appEventBus: AppEventBus
) : SelectFactorSourceProxy, SelectFactorSourceIOHandler {

    private var input: SelectFactorSourceInput = SelectFactorSourceInput.Init
    private val _output = MutableSharedFlow<SelectFactorSourceOutput>()

    override suspend fun selectFactorSource(context: SelectFactorSourceInput.Context): SelectFactorSourceOutput.Id? {
        input = SelectFactorSourceInput.WithContext(context)

        appEventBus.sendEvent(AppEvent.SelectFactorSource)
        val result = _output.first()

        return result as? SelectFactorSourceOutput.Id
    }

    override fun getInput(): SelectFactorSourceInput {
        return input
    }

    override suspend fun setOutput(output: SelectFactorSourceOutput) {
        _output.emit(output)
        reset()
    }

    private suspend fun reset() {
        input = SelectFactorSourceInput.Init
        _output.emit(SelectFactorSourceOutput.Init)
    }
}
