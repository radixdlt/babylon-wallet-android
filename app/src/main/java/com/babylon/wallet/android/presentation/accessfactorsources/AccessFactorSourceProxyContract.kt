package com.babylon.wallet.android.presentation.accessfactorsources

import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.derivation.model.NetworkId

// interface for clients that need access to factor sources
interface AccessFactorSourcesProxy {

    suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKey
    ): Result<AccessFactorSourcesOutput.PublicKeyAndDerivationPath>

    suspend fun reDeriveAccounts(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts
    ): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath>
}

// interface which acts as a mediator between the clients who need access to factor sources
// and the viewmodels of the bottom sheet dialogs
interface AccessFactorSourcesUiProxy {

    fun getInput(): AccessFactorSourcesInput

    suspend fun setOutput(output: AccessFactorSourcesOutput)
}

// ----- Models for input/output ----- //

sealed interface AccessFactorSourcesInput {

    data class ToDerivePublicKey(
        val forNetworkId: NetworkId,
        val factorSource: FactorSource.CreatingEntity? = null
    ) : AccessFactorSourcesInput

    sealed interface ToReDeriveAccounts : AccessFactorSourcesInput {

        val factorSource: FactorSource.CreatingEntity
        val isForLegacyOlympia: Boolean
        val nextDerivationPathOffset: Int // is used as pointer when user clicks "scan the next 50"

        data class WithGivenMnemonic(
            override val factorSource: FactorSource.CreatingEntity,
            override val isForLegacyOlympia: Boolean = false,
            override val nextDerivationPathOffset: Int,
            val mnemonicWithPassphrase: MnemonicWithPassphrase,
        ) : ToReDeriveAccounts

        data class WithGivenFactorSource(
            override val factorSource: FactorSource.CreatingEntity,
            override val isForLegacyOlympia: Boolean,
            override val nextDerivationPathOffset: Int,
        ) : ToReDeriveAccounts
    }

    data object Init : AccessFactorSourcesInput
}

sealed interface AccessFactorSourcesOutput {

    data class PublicKeyAndDerivationPath(
        val compressedPublicKey: ByteArray,
        val derivationPath: DerivationPath
    ) : AccessFactorSourcesOutput

    data class DerivedAccountsWithNextDerivationPath(
        val derivedAccounts: List<Network.Account>,
        val nextDerivationPathOffset: Int // is used as pointer when user clicks "scan the next 50"
    ) : AccessFactorSourcesOutput

    data class Failure(
        val error: Throwable
    ) : AccessFactorSourcesOutput

    data object Init : AccessFactorSourcesOutput
}
