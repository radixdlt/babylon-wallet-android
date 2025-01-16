package com.babylon.wallet.android.presentation.accessfactorsources

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.EntityKind
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable

/**
 * Interface for the callers (ViewModels or UseCases) that need access to factor sources.
 *
 * for example CreateAccountViewModel needs a public key to create an account therefore it must call:
 * val publicKey = accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(...)
 * createAccountUseCase(displayName = ""a name", publicKey = publicKey)
 *
 */
interface AccessFactorSourcesProxy {

    suspend fun getPublicKeyAndDerivationPathForFactorSource(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKey
    ): Result<AccessFactorSourcesOutput.HDPublicKey>

    suspend fun derivePublicKeys(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKeys
    ): AccessFactorSourcesOutput.DerivedPublicKeys

    suspend fun reDeriveAccounts(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts
    ): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath>

    suspend fun <SP : Signable.Payload, ID : Signable.ID> sign(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToSign<SP, ID>
    ): AccessFactorSourcesOutput.SignOutput<ID>

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

/**
 * Interface that is passed as parameter in the ViewModels of the bottom sheet dialogs.
 *
 * when a access-factor-source-bottom-sheet dialog pops up, then its viewmodel:
 * 1. takes the input from the accessFactorSourcesIOHandler.getInput()
 * 2. returns the output to the caller (e.g. CreateAccountViewModel) by calling accessFactorSourcesIOHandler.setOutput(...)
 *
 * This interface is also implemented in the AccessFactorSourcesProxyImpl.
 *
 */
interface AccessFactorSourcesIOHandler {

    fun getInput(): AccessFactorSourcesInput

    suspend fun setOutput(output: AccessFactorSourcesOutput)
}

// ----- Models for input/output ----- //

/**
 * Define in a clear manner with proper naming the minimum input and output.
 *
 * For example, when wallet needs to create an account,
 * we need to access factor sources in order to derive a public key,
 * therefore the public key is the (minimum) output and not the whole account!
 *
 */
sealed interface AccessFactorSourcesInput {

    data class ToDerivePublicKey(
        val entityKind: EntityKind,
        val forNetworkId: NetworkId,
        val factorSource: FactorSource,
        // Need this information only when a new profile is created, meaning that biometrics have been provided
        // No need to ask the user for authentication again.
        val isBiometricsProvided: Boolean
    ) : AccessFactorSourcesInput

    data class ToDerivePublicKeys(
        val purpose: DerivationPurpose,
        val request: KeyDerivationRequestPerFactorSource
    ) : AccessFactorSourcesInput

    sealed interface ToReDeriveAccounts : AccessFactorSourcesInput {

        val factorSource: FactorSource
        val isForLegacyOlympia: Boolean
        val nextDerivationPathIndex: HdPathComponent // is used as pointer when user clicks "scan the next 50"

        data class WithGivenMnemonic(
            override val factorSource: FactorSource,
            override val isForLegacyOlympia: Boolean = false,
            override val nextDerivationPathIndex: HdPathComponent,
            val mnemonicWithPassphrase: MnemonicWithPassphrase,
        ) : ToReDeriveAccounts

        data class WithGivenFactorSource(
            override val factorSource: FactorSource,
            override val isForLegacyOlympia: Boolean,
            override val nextDerivationPathIndex: HdPathComponent,
        ) : ToReDeriveAccounts
    }

    data class ToSign<SP : Signable.Payload, ID : Signable.ID>(
        val purpose: Purpose,
        val kind: FactorSourceKind,
        val input: PerFactorSourceInput<SP, ID>
    ) : AccessFactorSourcesInput {

        enum class Purpose {
            TransactionIntents,
            SubIntents,
            AuthIntents
        }
    }

    data object Init : AccessFactorSourcesInput
}

/**
 * Define in a clear manner with proper naming the minimum input and output.
 *
 * For example, when wallet needs to create an account,
 * we need to access factor sources in order to derive a public key,
 * therefore the public key is the (minimum) output and not the whole account!
 *
 */
sealed interface AccessFactorSourcesOutput {

    data class HDPublicKey(
        val value: HierarchicalDeterministicPublicKey
    ) : AccessFactorSourcesOutput

    sealed interface DerivedPublicKeys : AccessFactorSourcesOutput {
        data class Success(
            val factorSourceId: FactorSourceIdFromHash,
            val factorInstances: List<HierarchicalDeterministicFactorInstance>
        ) : DerivedPublicKeys

        data object Rejected : DerivedPublicKeys
    }

    data class DerivedAccountsWithNextDerivationPath(
        val derivedAccounts: List<Account>,
        val nextDerivationPathIndex: HdPathComponent // is used as pointer when user clicks "scan the next 50"
    ) : AccessFactorSourcesOutput

    sealed interface SignOutput<ID : Signable.ID> : AccessFactorSourcesOutput {
        data class Completed<ID : Signable.ID>(
            val outcome: PerFactorOutcome<ID>
        ) : SignOutput<ID>

        data object Rejected : SignOutput<Signable.ID>
    }

    data class Failure(
        val error: Throwable
    ) : AccessFactorSourcesOutput

    data object Init : AccessFactorSourcesOutput
}
