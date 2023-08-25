package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetRestoringProfileFromBackupUseCase
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class RestoreMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val getRestoringProfileFromBackupUseCase: GetRestoringProfileFromBackupUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<RestoreMnemonicsViewModel.State>(),
    OneOffEventHandler<RestoreMnemonicsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = RestoreMnemonicsArgs.from(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSources = when (args) {
                is RestoreMnemonicsArgs.RestoreProfile -> {
                    val profile = getRestoringProfileFromBackupUseCase()
                    val allAccounts = profile?.currentNetwork?.accounts.orEmpty()
                    profile?.factorSources
                        ?.filterIsInstance<DeviceFactorSource>()
                        ?.filter { !mnemonicRepository.mnemonicExist(it.id) }
                        ?.mapNotNull { factorSource ->
                            val associatedAccounts = allAccounts.filter { it.factorSourceId() == factorSource.id }

                            if (associatedAccounts.isEmpty() && !factorSource.isBabylon) return@mapNotNull null

                            RecoverableFactorSource(
                                associatedAccounts = associatedAccounts,
                                factorSource = factorSource
                            )
                        }
                        .orEmpty()
                }

                is RestoreMnemonicsArgs.RestoreSpecificMnemonic -> {
                    val profile = getProfileUseCase().firstOrNull() ?: return@launch
                    val allAccounts = profile.currentNetwork.accounts
                    profile.factorSources.filterIsInstance<DeviceFactorSource>().find { factorSource ->
                        factorSource.id.body.value == args.factorSourceIdHex && !mnemonicRepository.mnemonicExist(factorSource.id)
                    }?.let { factorSource ->
                        val associatedAccounts = allAccounts.filter { it.factorSourceId() == factorSource.id }

                        listOf(
                            RecoverableFactorSource(
                                associatedAccounts = associatedAccounts,
                                factorSource = factorSource
                            )
                        )
                    }.orEmpty()
                }
            }

            _state.update { it.copy(recoverableFactorSources = factorSources) }

            showNextRecoverableFactorSourceOrFinish()
        }

        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { it.copy(seedPhraseState = delegateState) }
            }
        }
    }

    fun onBackClick() {
        if (!state.value.isShowingEntities) {
            _state.update { it.copy(isShowingEntities = true, isMovingForward = false) }
        } else {
            viewModelScope.launch { sendEvent(Event.FinishRestoration(isMovingToMain = false)) }
        }
    }

    fun onSkipClick() {
        viewModelScope.launch { showNextRecoverableFactorSourceOrFinish() }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value) {
            sendEvent(Event.MoveToNextWord)
        }
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onSubmit() {
        if (state.value.isShowingEntities) {
            _state.update { it.copy(isShowingEntities = false, isMovingForward = false) }
        } else {
            viewModelScope.launch { restoreMnemonic() }
        }
    }

    private suspend fun restoreMnemonic() {
        val storedSecurityState = state.value
            .recoverableFactorSource
            ?.associatedAccounts
            ?.firstOrNull()
            ?.securityState as? SecurityState.Unsecured ?: return

        _state.update { it.copy(isRestoring = true) }
        restoreMnemonicUseCase(
            factorInstance = storedSecurityState.unsecuredEntityControl.transactionSigning,
            mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = _state.value.seedPhraseState.wordsPhrase,
                bip39Passphrase = _state.value.seedPhraseState.bip39Passphrase
            )
        ).onSuccess {
            if (args is RestoreMnemonicsArgs.RestoreProfile && _state.value.isMainSeedPhrase) {
                restoreProfileFromBackupUseCase()
            }

            appEventBus.sendEvent(AppEvent.RestoredMnemonic)
            _state.update { state -> state.copy(isRestoring = false) }
            showNextRecoverableFactorSourceOrFinish()
        }.onFailure {
            _state.update { state -> state.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic, isRestoring = false) }
        }
    }

    private suspend fun showNextRecoverableFactorSourceOrFinish() {
        val nextRecoverableFactorSource = state.value.nextRecoverableFactorSource
        if (nextRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(nextRecoverableFactorSource.factorSource.hint.mnemonicWordCount)

            _state.update { it.proceedToNextRecoverable() }
        } else {
            sendEvent(Event.FinishRestoration(isMovingToMain = args is RestoreMnemonicsArgs.RestoreProfile))
        }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val selectedIndex: Int = -1,
        val isShowingEntities: Boolean = true,
        val isMovingForward: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isRestoring: Boolean = false,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState {

        val nextRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex + 1)

        val recoverableFactorSource: RecoverableFactorSource?
            get() = if (selectedIndex == -1) null else recoverableFactorSources.getOrNull(selectedIndex)

        val isMainSeedPhrase: Boolean
            get() = recoverableFactorSource?.factorSource?.isBabylon == true

        fun proceedToNextRecoverable() = copy(
            selectedIndex = selectedIndex + 1,
            isMovingForward = true,
            isShowingEntities = true
        )
    }

    sealed interface Event : OneOffEvent {
        data class FinishRestoration(val isMovingToMain: Boolean) : Event
        object MoveToNextWord : Event
    }
}

data class RecoverableFactorSource(
    val associatedAccounts: List<Network.Account>,
    val factorSource: DeviceFactorSource
)
