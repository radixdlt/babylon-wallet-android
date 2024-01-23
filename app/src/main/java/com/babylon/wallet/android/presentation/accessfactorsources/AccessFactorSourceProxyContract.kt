package com.babylon.wallet.android.presentation.accessfactorsource

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.derivation.model.NetworkId

// interface for clients that need access to factor sources
interface AccessFactorSourceProxy {

    suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourceInput: AccessFactorSourceInput.ToCreateAccount
    ): AccessFactorSourceOutput.PublicKeyAndDerivationPath
}

// interface for the AccessFactorSourceViewModel that works as a mediator between the clients
// and the AccessFactorSourceProvider
interface AccessFactorSourceUiProxy {

    fun getInput(): AccessFactorSourceInput

    suspend fun setOutput(output: AccessFactorSourceOutput?)

    suspend fun reset()
}

// ----- Models for input/output ----- //

sealed interface AccessFactorSourceInput {

    data class ToCreateAccount(
        val forNetworkId: NetworkId,
        val factorSource: FactorSource.CreatingEntity? = null
    ) : AccessFactorSourceInput

    // just for demonstration - will change in next PR
    data class ToSign(
        val someData: List<Int>
    ) : AccessFactorSourceInput

    data object Init : AccessFactorSourceInput
}

sealed interface AccessFactorSourceOutput {

    data class PublicKeyAndDerivationPath(
        val compressedPublicKey: ByteArray,
        val derivationPath: DerivationPath
    ) : AccessFactorSourceOutput

    // just for demonstration - will change in next PR
    data class Signers(
        val someData: List<String>
    ) : AccessFactorSourceOutput
}
