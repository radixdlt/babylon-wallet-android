package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.domain.model.signing.SignRequest
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.EntityKind
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity

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

    suspend fun reDeriveAccounts(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToReDeriveAccounts
    ): Result<AccessFactorSourcesOutput.DerivedAccountsWithNextDerivationPath>

    suspend fun getSignatures(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToGetSignatures
    ): Result<AccessFactorSourcesOutput.EntitiesWithSignatures>

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

    data class ToGetSignatures(
        val signers: List<ProfileEntity>,
        val signRequest: SignRequest
    ) : AccessFactorSourcesInput

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

    data class DerivedAccountsWithNextDerivationPath(
        val derivedAccounts: List<Account>,
        val nextDerivationPathOffset: UInt // is used as pointer when user clicks "scan the next 50"
    ) : AccessFactorSourcesOutput

    data class EntitiesWithSignatures(
        val signersWithSignatures: Map<ProfileEntity, SignatureWithPublicKey>
    ) : AccessFactorSourcesOutput

    data class Failure(
        val error: Throwable
    ) : AccessFactorSourcesOutput

    data object Init : AccessFactorSourcesOutput
}
