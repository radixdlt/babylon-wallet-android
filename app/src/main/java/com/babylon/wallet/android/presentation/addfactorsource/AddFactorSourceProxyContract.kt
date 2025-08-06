package com.babylon.wallet.android.presentation.addfactorsource

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind

interface AddFactorSourceProxy {

    suspend fun addFactorSource(input: AddFactorSourceInput): AddFactorSourceOutput.Id?
}

interface AddFactorSourceIOHandler {

    fun getInput(): AddFactorSourceInput

    suspend fun setOutput(output: AddFactorSourceOutput)
}

sealed interface AddFactorSourceInput {

    data class WithKindPreselected(
        val kind: FactorSourceKind,
        val context: Context
    ) : AddFactorSourceInput

    data class FromKinds(
        val kinds: List<FactorSourceKind>,
        val context: Context
    ) : AddFactorSourceInput

    data class OfAnyKind(
        val context: Context
    ) : AddFactorSourceInput

    data object Init : AddFactorSourceInput

    fun context(): Context = when (this) {
        is WithKindPreselected -> context
        is FromKinds -> context
        is OfAnyKind -> context
        Init -> error("Context is not applicable for Init input")
    }

    sealed interface Context {

        data object New : Context

        data class Recovery(
            val isOlympia: Boolean
        ) : Context
    }
}

sealed interface AddFactorSourceOutput {

    data class Id(val value: FactorSourceId) : AddFactorSourceOutput

    data object Init : AddFactorSourceOutput
}
