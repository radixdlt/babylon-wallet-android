package com.babylon.wallet.android.presentation.addfactorsource

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind

interface AddFactorSourceProxy {

    suspend fun addFactorSource(kind: FactorSourceKind? = null): AddFactorSourceOutput.Id?
}

interface AddFactorSourceIOHandler {

    fun getInput(): AddFactorSourceInput

    suspend fun setOutput(output: AddFactorSourceOutput)
}

sealed interface AddFactorSourceInput {

    data class WithKind(val kind: FactorSourceKind?) : AddFactorSourceInput

    data object Init : AddFactorSourceInput
}

sealed interface AddFactorSourceOutput {

    data class Id(val value: FactorSourceId) : AddFactorSourceOutput

    data object Init : AddFactorSourceOutput
}
