package com.babylon.wallet.android.presentation.addfactorsource

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.MnemonicWithPassphrase

interface AddFactorSourceProxy {

    suspend fun addFactorSource(input: AddFactorSourceInput): AddFactorSourceOutput.Id?
}

interface AddFactorSourceIOHandler {

    fun getInput(): AddFactorSourceInput

    suspend fun setOutput(output: AddFactorSourceOutput)

    fun setIntermediaryParams(params: AddFactorSourceIntermediaryParams)

    fun getIntermediaryParams(): AddFactorSourceIntermediaryParams?
}

sealed interface AddFactorSourceInput {

    data class WithKind(
        val kind: FactorSourceKind,
        val context: Context
    ) : AddFactorSourceInput

    data class SelectKind(
        val kinds: List<FactorSourceKind>,
        val context: Context
    ) : AddFactorSourceInput

    data object Init : AddFactorSourceInput

    fun context(): Context = when (this) {
        is WithKind -> context
        is SelectKind -> context
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

sealed interface AddFactorSourceIntermediaryParams {

    data class Mnemonic(
        val value: MnemonicWithPassphrase
    ) : AddFactorSourceIntermediaryParams

    data class Ledger(
        val factorSourceId: FactorSourceId.Hash,
        val model: LedgerHardwareWalletModel
    ) : AddFactorSourceIntermediaryParams
}
