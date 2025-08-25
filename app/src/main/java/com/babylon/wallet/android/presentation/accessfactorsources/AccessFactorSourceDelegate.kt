package com.babylon.wallet.android.presentation.accessfactorsources

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToSignTransaction
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessArculusFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase.SeedPhraseValidity
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate.State.FactorSourcesToAccess
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH
import com.radixdlt.sargon.CommonException.SecureStorageAccessException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.isManualCancellation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase

@Suppress("LongParameterList", "TooManyFunctions")
class AccessFactorSourceDelegate private constructor(
    private val viewModelScope: CoroutineScope,
    private val input: DelegateInput,
    private val getProfileUseCase: GetProfileUseCase,
    private val accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
    private val accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
    private val defaultDispatcher: CoroutineDispatcher,
    private val onAccessCallback: suspend (FactorSource) -> Result<Unit>,
    private val onDismissCallback: suspend () -> Unit,
    private val onFailCallback: suspend () -> Unit,
) {

    constructor(
        viewModelScope: CoroutineScope,
        id: FactorSourceId,
        getProfileUseCase: GetProfileUseCase,
        accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
        accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
        defaultDispatcher: CoroutineDispatcher,
        onAccessCallback: suspend (FactorSource) -> Result<Unit>,
        onDismissCallback: suspend () -> Unit,
        onFailCallback: suspend () -> Unit,
    ) : this(
        viewModelScope = viewModelScope,
        input = DelegateInput.WithFactorSourceId(factorSourceId = id),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        accessArculusFactorSourceUseCase = accessArculusFactorSourceUseCase,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = onAccessCallback,
        onDismissCallback = onDismissCallback,
        onFailCallback = onFailCallback,
    )

    constructor(
        viewModelScope: CoroutineScope,
        factorSource: FactorSource,
        getProfileUseCase: GetProfileUseCase,
        accessOffDeviceMnemonicFactorSource: AccessOffDeviceMnemonicFactorSourceUseCase,
        accessArculusFactorSourceUseCase: AccessArculusFactorSourceUseCase,
        defaultDispatcher: CoroutineDispatcher,
        onAccessCallback: suspend (FactorSource) -> Result<Unit>,
        onDismissCallback: suspend () -> Unit,
        onFailCallback: suspend () -> Unit,
    ) : this(
        viewModelScope = viewModelScope,
        input = DelegateInput.WithFactorSource(factorSource = factorSource),
        getProfileUseCase = getProfileUseCase,
        accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
        accessArculusFactorSourceUseCase = accessArculusFactorSourceUseCase,
        defaultDispatcher = defaultDispatcher,
        onAccessCallback = onAccessCallback,
        onDismissCallback = onDismissCallback,
        onFailCallback = onFailCallback,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(
        State(
            factorSourceToAccess = when (input) {
                is DelegateInput.WithFactorSource -> FactorSourcesToAccess.Mono(factorSource = input.factorSource)
                is DelegateInput.WithFactorSourceId -> FactorSourcesToAccess.Resolving(id = input.factorSourceId)
            }
        )
    )
    val state: StateFlow<State>
        get() = _state.asStateFlow()

    private var accessJob: Job? = null
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    init {
        accessJob = viewModelScope.launch {
            when (val factorSourceToAccess = state.value.factorSourceToAccess) {
                is FactorSourcesToAccess.Mono -> access(factorSource = factorSourceToAccess.factorSource)
                is FactorSourcesToAccess.Resolving -> resolveFactorSourcesAndAccess(id = factorSourceToAccess.id)
            }
        }

        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update {
                    it.copy(seedPhraseInputState = it.seedPhraseInputState.copy(delegateState = delegateState))
                }
            }
        }
    }

    fun onDismiss() = viewModelScope.launch {
        skipSigning()
    }

    fun onSeedPhraseWordChanged(wordIndex: Int, word: String) {
        seedPhraseInputDelegate.onWordChanged(wordIndex, word)
    }

    fun onPasswordTyped(password: String) {
        _state.update { it.copy(passwordState = it.passwordState.copy(input = password)) }
    }

    fun onArculusPinChange(pin: String) {
        _state.update { it.copy(arculusPinState = it.arculusPinState.copy(input = pin)) }
    }

    fun onForgotArculusPinClick() {
        _state.update { it.copy(arculusPinState = it.arculusPinState.copy(showInfoMessage = true)) }
    }

    fun onArculusInfoMessageDismiss() {
        _state.update { it.copy(arculusPinState = it.arculusPinState.copy(showInfoMessage = false)) }
    }

    fun onRetry() {
        val factorSource = _state.value.factorSource ?: return

        accessJob?.cancel()
        accessJob = viewModelScope.launch {
            access(factorSource)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onInputConfirmed() = viewModelScope.launch {
        val factorSource = _state.value.factorSource ?: return@launch
        when (factorSource) {
            is FactorSource.OffDeviceMnemonic -> {
                val validity = accessOffDeviceMnemonicFactorSource.onSeedPhraseConfirmed(
                    factorSourceId = factorSource.value.id,
                    words = _state.value.seedPhraseInputState.inputWords
                )

                _state.update {
                    it.copy(
                        seedPhraseInputState = it.seedPhraseInputState.copy(
                            seedPhraseValidity = validity,
                            isConfirmButtonEnabled = validity == SeedPhraseValidity.Valid
                        )
                    )
                }
            }

            is FactorSource.ArculusCard -> {
                accessArculusFactorSourceUseCase.onPinForSigningConfirmed(_state.value.arculusPinState.input)
                access(factorSource)
            }

            is FactorSource.Password -> TODO("Future implementation")
            else -> {
                // The rest of the factor sources require no manual input
            }
        }
    }

    fun onCancelAccess() {
        accessJob?.cancel()
    }

    private suspend fun resolveFactorSourcesAndAccess(id: FactorSourceId) {
        val profile = getProfileUseCase()

        val factorSource = profile.factorSourceById(
            id = id
        ) ?: run {
            onFailCallback()
            return
        }

        access(factorSource)
    }

    private suspend fun access(factorSource: FactorSource) {
        _state.update {
            it.copy(
                isAccessInProgress = true,
                factorSourceToAccess = FactorSourcesToAccess.Mono(factorSource)
            )
        }

        if (factorSource is FactorSource.OffDeviceMnemonic) {
            setupSeedPhraseInput(factorSource)
        }

        onAccessCallback(factorSource)
            .onSuccess {
                _state.update { state -> state.copy(isAccessInProgress = false) }
            }.onFailure { error ->
                val errorMessageToShow =
                    if (error is SecureStorageAccessException && error.errorKind.isManualCancellation()) {
                        null
                    } else if (error is FailedToSignTransaction && error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction) {
                        null
                    } else {
                        UiMessage.ErrorMessage(error)
                    }

                _state.update {
                    it.copy(
                        isAccessInProgress = false,
                        errorMessage = errorMessageToShow,
                        arculusPinState = it.arculusPinState.copy(
                            input = ""
                        )
                    )
                }
            }
    }

    private suspend fun skipSigning() {
        accessJob?.cancel()
        onDismissCallback()
    }

    private fun setupSeedPhraseInput(factorSource: FactorSource.OffDeviceMnemonic) {
        // First set the input to the correct word count
        seedPhraseInputDelegate.setSeedPhraseSize(factorSource.value.hint.wordCount)

        // Then start observing the changes to the input, to enable/disable the confirm button
        _state
            .filter { it.factorSource is FactorSource.OffDeviceMnemonic }
            .distinctUntilChanged { old, new -> old.seedPhraseInputState.delegateState == new.seedPhraseInputState.delegateState }
            .onEach { newState ->
                val isComplete = newState.seedPhraseInputState.delegateState.isInputComplete()
                _state.update {
                    it.copy(
                        seedPhraseInputState = it.seedPhraseInputState.copy(
                            isConfirmButtonEnabled = isComplete,
                            seedPhraseValidity = null
                        )
                    )
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    private sealed interface DelegateInput {
        data class WithFactorSource(val factorSource: FactorSource) : DelegateInput

        data class WithFactorSourceId(val factorSourceId: FactorSourceId) : DelegateInput
    }

    data class State(
        val factorSourceToAccess: FactorSourcesToAccess,
        private val isAccessInProgress: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val seedPhraseInputState: SeedPhraseInputState = SeedPhraseInputState(),
        val passwordState: PasswordState = PasswordState(),
        val arculusPinState: ArculusPinState = ArculusPinState()
    ) : UiState {

        private val allowRetryWhenAccessInProgress = when (factorSourceToAccess.kind) {
            // We can click on retry when a request is in progress for ledger devices.
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> true
            else -> false
        }

        val isRetryEnabled: Boolean
            get() = !isAccessInProgress || allowRetryWhenAccessInProgress

        val factorSource: FactorSource? = when (factorSourceToAccess) {
            is FactorSourcesToAccess.Mono -> factorSourceToAccess.factorSource
            is FactorSourcesToAccess.Resolving -> null
        }

        sealed interface FactorSourcesToAccess {

            val id: FactorSourceId
            val kind: FactorSourceKind
                get() = when (val id = id) {
                    is FactorSourceId.Hash -> id.value.kind
                }

            data class Resolving(
                override val id: FactorSourceId
            ) : FactorSourcesToAccess

            data class Mono(
                val factorSource: FactorSource
            ) : FactorSourcesToAccess {
                override val id: FactorSourceId
                    get() = factorSource.id
            }
        }

        data class SeedPhraseInputState(
            val delegateState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
            val seedPhraseValidity: SeedPhraseValidity? = null,
            val isConfirmButtonEnabled: Boolean = false
        ) {

            val errorInSeedPhrase = seedPhraseValidity != null && seedPhraseValidity != SeedPhraseValidity.Valid

            val inputWords: ImmutableList<SeedPhraseWord> = delegateState.seedPhraseWords
        }

        data class PasswordState(
            val input: String = "",
            val isPasswordInvalidErrorVisible: Boolean = false
        )

        data class ArculusPinState(
            val input: String = "",
            val showInfoMessage: Boolean = false
        ) {

            val isConfirmButtonEnabled: Boolean = input.length == ARCULUS_PIN_LENGTH
        }
    }
}
