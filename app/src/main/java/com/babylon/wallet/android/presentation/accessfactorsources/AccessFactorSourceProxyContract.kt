package com.babylon.wallet.android.presentation.accessfactorsources

import com.radixdlt.sargon.AuthorizationPurpose
import com.radixdlt.sargon.AuthorizationResponse
import com.radixdlt.sargon.DerivationPurpose
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.KeyDerivationRequestPerFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.PerFactorOutcomeOfAuthIntentHash
import com.radixdlt.sargon.PerFactorOutcomeOfSubintentHash
import com.radixdlt.sargon.PerFactorOutcomeOfTransactionIntentHash
import com.radixdlt.sargon.PerFactorSourceInputOfAuthIntent
import com.radixdlt.sargon.PerFactorSourceInputOfSubintent
import com.radixdlt.sargon.PerFactorSourceInputOfTransactionIntent
import com.radixdlt.sargon.SpotCheckResponse
import com.radixdlt.sargon.TransactionSignRequestInputOfAuthIntent
import com.radixdlt.sargon.TransactionSignRequestInputOfSubintent
import com.radixdlt.sargon.TransactionSignRequestInputOfTransactionIntent
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash
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

    suspend fun requestAuthorization(
        input: AccessFactorSourcesInput.ToRequestAuthorization
    ): AccessFactorSourcesOutput.RequestAuthorization

    suspend fun derivePublicKeys(
        accessFactorSourcesInput: AccessFactorSourcesInput.ToDerivePublicKeys
    ): AccessFactorSourcesOutput.DerivedPublicKeys

    suspend fun sign(
        input: AccessFactorSourcesInput.Sign
    ): AccessFactorSourcesOutput.Signing

    suspend fun spotCheck(factorSource: FactorSource, allowSkip: Boolean): AccessFactorSourcesOutput.SpotCheckOutput

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

    data class ToRequestAuthorization(
        val purpose: AuthorizationPurpose
    ) : AccessFactorSourcesInput

    data class ToDerivePublicKeys(
        val purpose: DerivationPurpose,
        val request: KeyDerivationRequestPerFactorSource
    ) : AccessFactorSourcesInput

    sealed interface Sign: AccessFactorSourcesInput {
        val factorSourceId: FactorSourceIdFromHash
    }

    data class SignTransaction(
        override val factorSourceId: FactorSourceIdFromHash,
        val input: PerFactorSourceInputOfTransactionIntent
    ): Sign

    data class SignSubintent(
        override val factorSourceId: FactorSourceIdFromHash,
        val input: PerFactorSourceInputOfSubintent
    ): Sign

    data class SignAuth(
        override val factorSourceId: FactorSourceIdFromHash,
        val input: PerFactorSourceInputOfAuthIntent
    ): Sign

    data class ToSpotCheck(
        val factorSource: FactorSource,
        val allowSkip: Boolean
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

    data class RequestAuthorization(
        val output: AuthorizationResponse
    ) : AccessFactorSourcesOutput

    sealed interface DerivedPublicKeys : AccessFactorSourcesOutput {
        data class Success(
            val factorSourceId: FactorSourceIdFromHash,
            val factorInstances: List<HierarchicalDeterministicFactorInstance>
        ) : DerivedPublicKeys

        data object Rejected : DerivedPublicKeys
    }

    sealed interface Signing : AccessFactorSourcesOutput

    data class SignTransaction(
        val outcome: PerFactorOutcomeOfTransactionIntentHash
    ) : Signing

    data class SignSubintent(
        val outcome: PerFactorOutcomeOfSubintentHash
    ) : Signing

    data class SignAuth(
        val outcome: PerFactorOutcomeOfAuthIntentHash
    ) : Signing

    data object SigningRejected : Signing

    sealed interface SpotCheckOutput : AccessFactorSourcesOutput {
        data class Completed(
            val response: SpotCheckResponse
        ) : SpotCheckOutput

        data object Rejected : SpotCheckOutput
    }

    data object Init : AccessFactorSourcesOutput
}

fun TransactionSignRequestInputOfTransactionIntent.payloadId() = payload.decompile().hash()
fun TransactionSignRequestInputOfSubintent.payloadId() = payload.decompile().hash()
fun TransactionSignRequestInputOfAuthIntent.payloadId() = payload.hash()
