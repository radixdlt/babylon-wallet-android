package com.babylon.wallet.android.presentation.selectfactorsource

import com.radixdlt.sargon.FactorSourceId

interface SelectFactorSourceProxy {

    suspend fun selectFactorSource(context: SelectFactorSourceInput.Context): SelectFactorSourceOutput.Id?
}

interface SelectFactorSourceIOHandler {

    fun getInput(): SelectFactorSourceInput

    suspend fun setOutput(output: SelectFactorSourceOutput)
}

sealed interface SelectFactorSourceInput {

    data class WithContext(val context: Context) : SelectFactorSourceInput

    data object Init : SelectFactorSourceInput

    sealed interface Context {

        data object CreateAccount : Context

        data object CreatePersona : Context

        data class AccountRecovery(
            val isOlympia: Boolean
        ) : Context
    }
}

sealed interface SelectFactorSourceOutput {

    data class Id(val value: FactorSourceId) : SelectFactorSourceOutput

    data object Init : SelectFactorSourceOutput
}
