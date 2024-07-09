package com.babylon.wallet.android.presentation.accessfactorsources

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData

// interface for clients that need access to factor sources
interface AccessFactorSourcesProxy {

    suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKey
    ): Result<AccessFactorSourcesOutput.HDPublicKey>

    suspend fun reDeriveAccounts(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts
    ): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath>

    suspend fun createPersona(
        accessFactorSourcesInput: AccessFactorSourcesInput.CreatePersona
    ): Result<AccessFactorSourcesOutput.CreatedPersona>

    /**
     * This method temporarily keeps in memory the mnemonic that has been added through
     * the Account Recovery Scan in the onboarding flow.
     *
     */
    fun setTempMnemonicWithPassphrase(mnemonicWithPassphrase: MnemonicWithPassphrase)

    /**
     * This method returns the mnemonic that has been added through
     * the Account Recovery Scan in the onboarding flow.
     *
     */
    fun getTempMnemonicWithPassphrase(): MnemonicWithPassphrase?
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
        val factorSource: FactorSource,
        // Need this information only when a new profile is created, meaning that biometrics have been provided
        // No need to ask the user for authentication again.
        val isBiometricsProvided: Boolean
    ) : AccessFactorSourcesInput

    sealed interface ToReDeriveAccounts : AccessFactorSourcesInput {

        val factorSource: FactorSource
        val isForLegacyOlympia: Boolean
        val nextDerivationPathOffset: UInt // is used as pointer when user clicks "scan the next 50"

        data class WithGivenMnemonic(
            override val factorSource: FactorSource,
            override val isForLegacyOlympia: Boolean = false,
            override val nextDerivationPathOffset: UInt,
            val mnemonicWithPassphrase: MnemonicWithPassphrase,
        ) : ToReDeriveAccounts

        data class WithGivenFactorSource(
            override val factorSource: FactorSource,
            override val isForLegacyOlympia: Boolean,
            override val nextDerivationPathOffset: UInt,
        ) : ToReDeriveAccounts
    }

    data class CreatePersona(val displayName: DisplayName, val personaData: PersonaData) : AccessFactorSourcesInput

    data object Init : AccessFactorSourcesInput
}

sealed interface AccessFactorSourcesOutput {

    data class HDPublicKey(
        val value: HierarchicalDeterministicPublicKey
    ) : AccessFactorSourcesOutput

    data class DerivedAccountsWithNextDerivationPath(
        val derivedAccounts: List<Account>,
        val nextDerivationPathOffset: UInt // is used as pointer when user clicks "scan the next 50"
    ) : AccessFactorSourcesOutput

    data class CreatedPersona(val persona: Persona) : AccessFactorSourcesOutput

    data class Failure(
        val error: Throwable
    ) : AccessFactorSourcesOutput

    data object Init : AccessFactorSourcesOutput
}
