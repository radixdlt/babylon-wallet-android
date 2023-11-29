package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.changeGateway
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.isHidden
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceFlag
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

    override fun initialState(): State = State(
        isMandatory = (args as? RestoreMnemonicsArgs.RestoreProfile)?.isMandatory == true
    )

    init {
        viewModelScope.launch {
            when (args) {
                is RestoreMnemonicsArgs.RestoreProfile -> {
                    val profile = args.backupType?.let { backupType ->
                        getTemporaryRestoringProfileForBackupUseCase(backupType)?.changeGateway(Radix.Gateway.mainnet)
                    } ?: run {
                        getProfileUseCase().firstOrNull()
                    }
                    val factorSources = profile.recoverableFactorSources()
                    _state.update {
                        it.copy(
                            recoverableFactorSources = factorSources,
                            mainBabylonFactorSourceId = profile?.mainBabylonFactorSourceId()
                        )
                    }
                }
            }

            showNextRecoverableFactorSourceOrFinish()
        }

        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { it.copy(seedPhraseState = delegateState) }
            }
        }
    }

    private suspend fun Profile?.recoverableFactorSources(): List<RecoverableFactorSource> {
        val allAccounts = this?.currentNetwork?.accounts.orEmpty()

        return this?.factorSources
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

    private fun Profile.mainBabylonFactorSourceId(): FactorSource.FactorSourceID.FromHash? {
        val deviceFactorSources = factorSources.filterIsInstance<DeviceFactorSource>()
        val babylonFactorSources = deviceFactorSources.filter { it.isBabylon }
        return if (babylonFactorSources.size == 1) {
            babylonFactorSources.first().id
        } else {
            babylonFactorSources.firstOrNull { it.common.flags.contains(FactorSourceFlag.Main) }?.id
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

    fun onSkipSeedPhraseClick(biometricAuthProvider: suspend () -> Boolean) {
        viewModelScope.launch { showNextRecoverableFactorSourceOrFinish(biometricAuthProvider) }
    }

    fun onSkipMainSeedPhraseClick() {
        _state.update {
            it.copy(screenType = State.ScreenType.NoMainSeedPhrase)
        }
    }

    fun skipMainSeedPhraseAndCreateNew(biometricAuthProvider: suspend () -> Boolean) {
        viewModelScope.launch {
            _state.update { state -> state.copy(hasSkippedMainSeedPhrase = true) }
            showNextRecoverableFactorSourceOrFinish(biometricAuthProvider)
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
            if (args is RestoreMnemonicsArgs.RestoreProfile) {
                args.backupType?.let { backupType ->
                    restoreProfileFromBackupUseCase(backupType)
                }
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

    @Suppress("NestedBlockDepth")
    private suspend fun showNextRecoverableFactorSourceOrFinish(biometricAuthProvider: suspend () -> Boolean = { true }) {
        val nextRecoverableFactorSource = state.value.nextRecoverableFactorSource
        if (nextRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(nextRecoverableFactorSource.factorSource.hint.mnemonicWordCount)

            _state.update { it.proceedToNextRecoverable() }
        } else {
            if (_state.value.hasSkippedMainSeedPhrase) {
                _state.update { it.copy(isRestoring = true) }

                (args as? RestoreMnemonicsArgs.RestoreProfile)?.backupType?.let { backupType ->
                    if (biometricAuthProvider().not()) return
                    restoreAndCreateMainSeedPhraseUseCase(backupType)
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
        private val mainBabylonFactorSourceId: FactorSource.FactorSourceID.FromHash? = null,
        private val selectedIndex: Int = -1,
        val screenType: ScreenType = ScreenType.Entities,
        val isMovingForward: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isRestoring: Boolean = false,
        val hasSkippedMainSeedPhrase: Boolean = false,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val isMandatory: Boolean = false
    ) : UiState {

        sealed interface ScreenType {
            data object Entities : ScreenType
            data object SeedPhrase : ScreenType
            data object NoMainSeedPhrase : ScreenType
        }

        val nextRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex + 1)

        val recoverableFactorSource: RecoverableFactorSource?
            get() = if (selectedIndex == -1) null else recoverableFactorSources.getOrNull(selectedIndex)

        val isMainBabylonSeedPhrase: Boolean
            get() = recoverableFactorSource?.factorSource?.id == mainBabylonFactorSourceId

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
) {
    val nonHiddenAccountsToDisplay: List<Network.Account>
        get() = associatedAccounts.filter { it.isHidden().not() }

    val allAccountsHidden: Boolean
        get() = associatedAccounts.all { it.isHidden() }
}
