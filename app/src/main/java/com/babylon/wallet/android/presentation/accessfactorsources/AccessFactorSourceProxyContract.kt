package com.babylon.wallet.android.presentation.accessfactorsources

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.derivation.model.NetworkId

// interface for clients that need access to factor sources
interface AccessFactorSourcesProxy {

    suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToCreateAccount
    ): Result<AccessFactorSourcesOutput.PublicKeyAndDerivationPath>
}

// interface for the AccessFactorSourceViewModel that works as a mediator between the clients
// and the AccessFactorSourcesProvider
interface AccessFactorSourcesUiProxy {

    fun getInput(): AccessFactorSourcesInput

    suspend fun setOutput(output: AccessFactorSourcesOutput)

    suspend fun reset()
}

// ----- Models for input/output ----- //

sealed interface AccessFactorSourcesInput {

    data class ToCreateAccount(
        val forNetworkId: NetworkId,
        val factorSource: FactorSource.CreatingEntity? = null
    ) : AccessFactorSourcesInput

    // just for demonstration - will change in next PR
    data class ToSign(
        val someData: List<Int>
    ) : AccessFactorSourcesInput

    data object Init : AccessFactorSourcesInput
}

sealed interface AccessFactorSourcesOutput {

    data class PublicKeyAndDerivationPath(
        val compressedPublicKey: ByteArray,
        val derivationPath: DerivationPath
    ) : AccessFactorSourcesOutput

    // just for demonstration - will change in next PR
    data class Signers(
        val someData: List<String>
    ) : AccessFactorSourcesOutput

    data class Failure(
        val error: Throwable
    ) : AccessFactorSourcesOutput

    data object Init : AccessFactorSourcesOutput
}
