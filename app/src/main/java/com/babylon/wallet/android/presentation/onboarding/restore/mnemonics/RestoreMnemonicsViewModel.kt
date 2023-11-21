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
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.changeGateway
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.RestoreAndCreateMainSeedPhraseUseCase
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class RestoreMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val restoreAndCreateMainSeedPhraseUseCase: RestoreAndCreateMainSeedPhraseUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
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
                    val profile = getTemporaryRestoringProfileForBackupUseCase(args.backupType)?.changeGateway(Radix.Gateway.mainnet)
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
                        factorSource.id.body == args.factorSourceId && !mnemonicRepository.mnemonicExist(factorSource.id)
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
        if (state.value.screenType != State.ScreenType.Entities) {
            _state.update { it.copy(screenType = State.ScreenType.Entities, isMovingForward = false) }
        } else {
            viewModelScope.launch {
                when (args) {
                    is RestoreMnemonicsArgs.RestoreProfile -> {
                        if (args.backupType is BackupType.File) {
                            discardTemporaryRestoredFileForBackupUseCase(BackupType.File.PlainText)
                        }

                        sendEvent(Event.FinishRestoration(isMovingToMain = false))
                    }
                    is RestoreMnemonicsArgs.RestoreSpecificMnemonic -> {
                        if (args.isMandatory) {
                            sendEvent(Event.CloseApp)
                        } else {
                            sendEvent(Event.FinishRestoration(isMovingToMain = false))
                        }
                    }
                }
            }
        }
    }

    fun onSkipSeedPhraseClick() {
        viewModelScope.launch { showNextRecoverableFactorSourceOrFinish() }
    }

    fun onSkipMainSeedPhraseClick() {
        _state.update {
            it.copy(screenType = State.ScreenType.NoMainSeedPhrase)
        }
    }

    fun skipMainSeedPhraseAndCreateNew() {
        viewModelScope.launch {
            _state.update { state -> state.copy(hasSkippedMainSeedPhrase = true) }
            showNextRecoverableFactorSourceOrFinish()
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value) {
            sendEvent(Event.MoveToNextWord)
        }
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
        viewModelScope.launch {
            sendEvent(Event.MoveToNextWord)
        }
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onSubmit() {
        if (state.value.screenType == State.ScreenType.Entities) {
            _state.update { it.copy(screenType = State.ScreenType.SeedPhrase, isMovingForward = false) }
        } else {
            viewModelScope.launch { restoreMnemonic() }
        }
    }

    private suspend fun restoreMnemonic() {
        val factorSourceToRecover = state.value
            .recoverableFactorSource?.factorSource ?: return

        _state.update { it.copy(isRestoring = true) }
        restoreMnemonicUseCase(
            factorSource = factorSourceToRecover,
            mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = _state.value.seedPhraseState.wordsPhrase,
                bip39Passphrase = _state.value.seedPhraseState.bip39Passphrase
            )
        ).onSuccess {
            if (args is RestoreMnemonicsArgs.RestoreProfile && _state.value.isMainBabylonSeedPhrase) {
                restoreProfileFromBackupUseCase(args.backupType)
            }

            appEventBus.sendEvent(AppEvent.RestoredMnemonic)
            _state.update { state -> state.copy(isRestoring = false) }
            showNextRecoverableFactorSourceOrFinish()
        }.onFailure {
            _state.update { state ->
                state.copy(
                    uiMessage = UiMessage.InfoMessage.InvalidMnemonic,
                    isRestoring = false
                )
            }
        }
    }

    private suspend fun showNextRecoverableFactorSourceOrFinish() {
        val nextRecoverableFactorSource = state.value.nextRecoverableFactorSource
        if (nextRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(nextRecoverableFactorSource.factorSource.hint.mnemonicWordCount)

            _state.update { it.proceedToNextRecoverable() }
        } else {
            if (_state.value.hasSkippedMainSeedPhrase) {
                // Create main babylon seedphrase as it was skipped before
                _state.update { it.copy(isRestoring = true) }
                if (args is RestoreMnemonicsArgs.RestoreProfile) {
                    restoreAndCreateMainSeedPhraseUseCase(args.backupType)
                }

                _state.update { state ->
                    state.copy(
                        isRestoring = false,
                        hasSkippedMainSeedPhrase = false
                    )
                }
            }

            sendEvent(Event.FinishRestoration(isMovingToMain = args is RestoreMnemonicsArgs.RestoreProfile))
        }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val selectedIndex: Int = -1,
        val screenType: ScreenType = ScreenType.Entities,
        val isMovingForward: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isRestoring: Boolean = false,
        val hasSkippedMainSeedPhrase: Boolean = false,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState {

        sealed interface ScreenType {
            data object Entities : ScreenType
            data object SeedPhrase : ScreenType
            data object NoMainSeedPhrase : ScreenType
        }

        val nextRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex + 1)

        val isLastRecoverableFactorSource: Boolean
            get() = nextRecoverableFactorSource == null

        val recoverableFactorSource: RecoverableFactorSource?
            get() = if (selectedIndex == -1) null else recoverableFactorSources.getOrNull(selectedIndex)

        val isMainBabylonSeedPhrase: Boolean
            // If its only one factor source we treat it as main
            // If more than one we check against isMainBabylon
            get() = if (recoverableFactorSources.size == 1) {
                true
            } else {
                recoverableFactorSource?.factorSource?.isMainBabylon == true
            }

        fun proceedToNextRecoverable() = copy(
            selectedIndex = selectedIndex + 1,
            isMovingForward = true,
            screenType = ScreenType.Entities
        )
    }

    sealed interface Event : OneOffEvent {
        data class FinishRestoration(val isMovingToMain: Boolean) : Event
        data object CloseApp : Event
        data object MoveToNextWord : Event
    }
}

data class RecoverableFactorSource(
    val associatedAccounts: List<Network.Account>,
    val factorSource: DeviceFactorSource
)
