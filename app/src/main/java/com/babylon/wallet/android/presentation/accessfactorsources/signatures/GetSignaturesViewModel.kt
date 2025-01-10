package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessDeviceFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessLedgerHardwareWalletFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessOffDeviceMnemonicFactorSource
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.radixdlt.sargon.CommonException.SecureStorageAccessException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.NeglectFactorReason
import com.radixdlt.sargon.NeglectedFactor
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.isManualCancellation
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.Signable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class GetSignaturesViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val accessDeviceFactorSource: AccessDeviceFactorSource,
    private val accessLedgerHardwareWalletFactorSource: AccessLedgerHardwareWalletFactorSource,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSource,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<GetSignaturesViewModel.State>(),
    OneOffEventHandler<GetSignaturesViewModel.Event> by OneOffEventHandlerImpl() {

    @Suppress("UNCHECKED_CAST")
    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToSign<Signable.Payload, Signable.ID>

    private var signingJob: Job? = null
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State(
        signPurpose = proxyInput.purpose,
        factorSourceToSign = State.FactorSourcesToSign.Resolving(kind = proxyInput.kind)
    )

    init {
        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { it.copy(seedPhraseInputState = delegateState) }

                val seedPhrase = delegateState.validSeedPhraseOrNull()
                if (seedPhrase != null) {
                    accessOffDeviceMnemonicFactorSource.onSeedPhraseProvided(seedPhrase = seedPhrase)
                }
            }
        }

        signingJob = viewModelScope.launch {
            resolveFactorSourcesAndSign()
        }
    }

    fun onDismiss() = viewModelScope.launch {
        finishWithFailure(
            factorSourceId = proxyInput.input.factorSourceId,
            neglectFactorReason = NeglectFactorReason.USER_EXPLICITLY_SKIPPED
        )
    }

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) {
        seedPhraseInputDelegate.onWordChanged(wordIndex, word)
    }

    fun onPasswordTyped(password: String) {
        _state.update { it.copy(passwordInput = password) }
    }

    fun onRetry() {
        val factorSource = _state.value.factorSource ?: return

        signingJob?.cancel()
        signingJob = viewModelScope.launch {
            sign(factorSource)
        }
    }

    fun onSkip() {

    }

    fun onMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun resolveFactorSourcesAndSign() {
        val profile = getProfileUseCase()

        val factorSource = profile.factorSourceById(
            id = proxyInput.input.factorSourceId.asGeneral()
        ) ?: run {
            finishWithFailure(proxyInput.input.factorSourceId, NeglectFactorReason.FAILURE)
            return
        }

        sign(factorSource)
    }

    private suspend fun sign(factorSource: FactorSource) {
        _state.update {
            it.copy(
                isSigningInProgress = true,
                factorSourceToSign = State.FactorSourcesToSign.Mono(factorSource),
                seedPhraseInputState = if (factorSource is FactorSource.OffDeviceMnemonic) {
                    it.seedPhraseInputState.setSeedPhraseSize(factorSource.value.hint.wordCount)
                } else {
                    it.seedPhraseInputState
                }
            )
        }

        when (factorSource) {
            is FactorSource.Device -> accessDeviceFactorSource.signMono(
                factorSource = factorSource,
                input = proxyInput.input
            )
            is FactorSource.Ledger -> accessLedgerHardwareWalletFactorSource.signMono(
                factorSource = factorSource,
                input = proxyInput.input
            )
            is FactorSource.ArculusCard -> TODO()
            is FactorSource.OffDeviceMnemonic -> accessOffDeviceMnemonicFactorSource.signMono(
                factorSource = factorSource,
                input = proxyInput.input
            )
            is FactorSource.Password -> TODO()
            is FactorSource.SecurityQuestions -> error("Signing with ${factorSource.value.kind} is not supported yet")
            is FactorSource.TrustedContact -> error("Signing with ${factorSource.value.kind} is not supported yet")
        }.onSuccess { perFactorOutcome ->
            _state.update { it.copy(isSigningInProgress = false) }
            finishWithSuccess(perFactorOutcome)
        }.onFailure { error ->
            val errorMessageToShow = if (error is SecureStorageAccessException && error.errorKind.isManualCancellation()) {
                null
            } else if (error is FailedToSignTransaction && error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction) {
                null
            } else {
                UiMessage.ErrorMessage(error)
            }

            _state.update { it.copy(isSigningInProgress = false, errorMessage = errorMessageToShow) }
        }
    }

    private suspend fun finishWithSuccess(outcome: PerFactorOutcome<Signable.ID>) {
        sendEvent(event = Event.Completed)
        accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.SignOutput(output = outcome))
    }

    private suspend fun finishWithFailure(
        factorSourceId: FactorSourceIdFromHash,
        neglectFactorReason: NeglectFactorReason
    ) {
        signingJob?.cancel()

        // end the signing process and return the output (error)
        sendEvent(event = Event.Completed)

        val outcome = FactorOutcome.Neglected<Signable.ID>(
            factor = NeglectedFactor(
                reason = neglectFactorReason,
                factor = factorSourceId
            )
        )
        accessFactorSourcesIOHandler.setOutput(
            AccessFactorSourcesOutput.SignOutput(
                output = PerFactorOutcome(
                    factorSourceId = factorSourceId,
                    outcome = outcome
                )
            )
        )
    }

    data class State(
        val signPurpose: AccessFactorSourcesInput.ToSign.Purpose,
        val factorSourceToSign: FactorSourcesToSign,
        val isSigningInProgress: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val seedPhraseInputState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val passwordInput: String = ""
    ) : UiState {

        val factorSource: FactorSource? = when (factorSourceToSign) {
            is FactorSourcesToSign.Mono -> factorSourceToSign.factorSource
            is FactorSourcesToSign.Resolving -> null
        }

        sealed interface FactorSourcesToSign {

            val kind: FactorSourceKind

            data class Resolving(
                override val kind: FactorSourceKind
            ) : FactorSourcesToSign

            data class Mono(
                val factorSource: FactorSource
            ) : FactorSourcesToSign {
                override val kind: FactorSourceKind
                    get() = factorSource.kind
            }
        }
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
