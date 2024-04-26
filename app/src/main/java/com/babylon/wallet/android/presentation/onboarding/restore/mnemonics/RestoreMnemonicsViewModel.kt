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
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.invoke
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.isHidden
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.core.sargon.supportsBabylon
import rdx.works.core.sargon.usesEd25519
import rdx.works.core.sargon.usesSECP256k1
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
    lateinit var biometricAuthProvider: suspend () -> Boolean

    override fun initialState(): State = State(
        isMandatory = args.isMandatory
    )

    init {
        viewModelScope.launch {
            val profile = args.backupType?.let { backupType ->
                getTemporaryRestoringProfileForBackupUseCase(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)
            } ?: run {
                getProfileUseCase.flow.firstOrNull()
            }
            val factorSources = profile.recoverableFactorSources()
            _state.update {
                it.copy(
                    recoverableFactorSources = factorSources,
                    mainBabylonFactorSourceId = profile?.mainBabylonFactorSource?.value?.id?.asGeneral()
                )
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
        val allAccounts = this?.currentNetwork?.accounts().orEmpty()

        return this?.factorSources()
            ?.filterIsInstance<FactorSource.Device>()
            ?.filter { !mnemonicRepository.mnemonicExist(it.value.id.asGeneral()) }
            ?.mapNotNull { factorSource ->
                val associatedBabylonAccounts = allAccounts.filter {
                    it.factorSourceId == factorSource.id && it.usesEd25519
                }
                val associatedOlympiaAccounts = allAccounts.filter {
                    it.factorSourceId == factorSource.id && it.usesSECP256k1
                }

                if (associatedBabylonAccounts.isEmpty() && associatedOlympiaAccounts.isEmpty() && !factorSource.supportsBabylon) {
                    return@mapNotNull null
                }

                RecoverableFactorSource(
                    associatedAccounts = associatedBabylonAccounts.ifEmpty { associatedOlympiaAccounts },
                    factorSource = factorSource
                )
            }
            .orEmpty()
    }

    fun onBackClick() {
        if (state.value.screenType != State.ScreenType.Entities) {
            _state.update { it.copy(screenType = State.ScreenType.Entities, isMovingForward = false) }
        } else {
            viewModelScope.launch {
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
        if (biometricAuthProvider.invoke().not()) return
        _state.update { it.copy(isRestoring = true) }
        restoreMnemonicUseCase(
            factorSource = factorSourceToRecover,
            mnemonicWithPassphrase = _state.value.seedPhraseState.toMnemonicWithPassphrase()
        ).onSuccess {
            appEventBus.sendEvent(AppEvent.RestoredMnemonic)
            _state.update { state -> state.copy(isRestoring = false) }
            showNextRecoverableFactorSourceOrFinish(skipAuth = true)
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
    private suspend fun showNextRecoverableFactorSourceOrFinish(skipAuth: Boolean = false) {
        val nextRecoverableFactorSource = state.value.nextRecoverableFactorSource
        if (nextRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(nextRecoverableFactorSource.factorSource.value.hint.mnemonicWordCount)

            _state.update { it.proceedToNextRecoverable() }
        } else {
            if (_state.value.hasSkippedMainSeedPhrase) {
                _state.update { it.copy(isRestoring = true) }

                args.backupType?.let { backupType ->
                    if (skipAuth.not() && biometricAuthProvider().not()) return
                    restoreAndCreateMainSeedPhraseUseCase(backupType)
                }

                _state.update { state ->
                    state.copy(
                        isRestoring = false,
                        hasSkippedMainSeedPhrase = false
                    )
                }
            } else {
                args.backupType?.let { backupType ->
                    restoreProfileFromBackupUseCase(backupType)
                }
            }

            sendEvent(Event.FinishRestoration(isMovingToMain = true))
        }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val mainBabylonFactorSourceId: FactorSourceId.Hash? = null,
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
    val associatedAccounts: List<Account>,
    val factorSource: FactorSource.Device
) {
    val nonHiddenAccountsToDisplay: List<Account>
        get() = associatedAccounts.filter { it.isHidden.not() }

    val areAllAccountsHidden: Boolean
        get() = associatedAccounts.all { it.isHidden }
}
