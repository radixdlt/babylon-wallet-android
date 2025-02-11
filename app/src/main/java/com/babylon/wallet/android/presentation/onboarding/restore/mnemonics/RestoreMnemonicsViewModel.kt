package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
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
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIntegrity
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.isLegacy
import com.radixdlt.sargon.extensions.isMain
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.KeystoreManager
import rdx.works.core.sargon.active
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.isHidden
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class RestoreMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
    private val appEventBus: AppEventBus,
    private val homeCardsRepository: HomeCardsRepository,
    private val keystoreManager: KeystoreManager,
    private val sargonOsManager: SargonOsManager,
) : StateViewModel<RestoreMnemonicsViewModel.State>(),
    OneOffEventHandler<RestoreMnemonicsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = RestoreMnemonicsArgs.from(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)
    lateinit var biometricAuthProvider: suspend () -> Boolean

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToCheck = args.backupType?.let { backupType ->
                // Reset keyspec before importing any mnemonic, when keyspec is invalid
                keystoreManager.resetMnemonicKeySpecWhenInvalidated()

                getTemporaryRestoringProfileForBackupUseCase(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)?.let {
                    ProfileToCheck.Specific(it)
                }
            } ?: ProfileToCheck.Current

            val recoverableFactorSources = profileToCheck.recoverableFactorSources()
                // we want main factor source to go first
                .sortedByDescending {
                    it.factorSource.isMain
                }

            val mainBDFS = recoverableFactorSources.find { it.factorSource.isMain }?.factorSource
            _state.update {
                it.copy(
                    recoverableFactorSources = recoverableFactorSources,
                    mainBabylonFactorSourceId = mainBDFS?.value?.id?.asGeneral()
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

    private suspend fun ProfileToCheck.recoverableFactorSources(): List<RecoverableFactorSource> {
        val profile = when (this) {
            ProfileToCheck.Current -> getProfileUseCase()
            is ProfileToCheck.Specific -> this.v1
        }

        return profile.deviceFactorSources.map { deviceFactorSource ->
            sargonOsManager.sargonOs.entitiesLinkedToFactorSource(
                factorSource = deviceFactorSource,
                profileToCheck = this
            ) to deviceFactorSource
        }.filter {
            (it.first.integrity as? FactorSourceIntegrity.Device)?.v1?.isMnemonicPresentInSecureStorage == false
        }.map { (entities, deviceFactorSource) ->
            val babylonAccounts = entities.accounts.filter { !it.isLegacy }
            val olympiaAccounts = entities.accounts.filter { it.isLegacy }

            RecoverableFactorSource(
                associatedAccounts = babylonAccounts.ifEmpty { olympiaAccounts },
                factorSource = deviceFactorSource
            )
        }
    }

    fun onBackClick() {
        if (state.value.screenType != State.ScreenType.Entities) {
            _state.update { it.copy(screenType = State.ScreenType.Entities, isMovingForward = false) }
        } else {
            viewModelScope.launch {
                if (args.backupType is BackupType.File) {
                    discardTemporaryRestoredFileForBackupUseCase(BackupType.File.PlainText)
                }
                sendEvent(Event.FinishRestoration(isMovingToMain = false))
            }
        }
    }

    fun onSkipSeedPhraseClick() {
        viewModelScope.launch { showNextRecoverableFactorSourceOrFinish() }
    }

    fun onSkipMainSeedPhraseClick() {
        _state.update {
            it.copy(
                screenType = State.ScreenType.NoMainSeedPhrase,
                isMovingForward = true
            )
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
        seedPhraseInputDelegate.onWordChanged(index, value)
    }

    fun onWordSelected(index: Int, value: String) {
        seedPhraseInputDelegate.onWordSelected(index, value)
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onSubmit() {
        if (state.value.screenType == State.ScreenType.Entities) {
            _state.update {
                it.copy(
                    screenType = State.ScreenType.SeedPhrase,
                    isMovingForward = true
                )
            }
        } else {
            viewModelScope.launch { restoreMnemonic() }
        }
    }

    private suspend fun restoreMnemonic() {
        val factorSourceToRecover = state.value
            .recoverableFactorSource?.factorSource ?: return
        if (biometricAuthProvider.invoke().not()) return
        _state.update { it.copy(isPrimaryButtonLoading = true) }
        restoreMnemonicUseCase(
            factorSource = factorSourceToRecover,
            mnemonicWithPassphrase = _state.value.seedPhraseState.toMnemonicWithPassphrase()
        ).onSuccess {
            appEventBus.sendEvent(AppEvent.RestoredMnemonic)
            _state.update { state -> state.copy(isPrimaryButtonLoading = false) }
            showNextRecoverableFactorSourceOrFinish(skipAuth = true)
        }.onFailure { error ->
            if (error is ProfileException.SecureStorageAccess) {
                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
            } else {
                _state.update { state -> state.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
            _state.update { state -> state.copy(isPrimaryButtonLoading = false) }
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
                _state.update { it.copy(isPrimaryButtonLoading = true) }

                args.backupType?.let { backupType ->
                    if (skipAuth.not() && biometricAuthProvider().not()) {
                        _state.update { it.copy(isPrimaryButtonLoading = false) }
                        return
                    }

                    restoreProfileFromBackupUseCase(backupType = backupType, mainSeedPhraseSkipped = true)
                        .onSuccess {
                            _state.update { state ->
                                state.copy(
                                    isPrimaryButtonLoading = false,
                                    hasSkippedMainSeedPhrase = false
                                )
                            }
                            onRestorationComplete()
                        }.onFailure {
                            if (it is CommonException.SecureStorageWriteException) {
                                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                                _state.update { state ->
                                    state.copy(isPrimaryButtonLoading = false)
                                }
                            } else {
                                _state.update { state ->
                                    state.copy(
                                        isPrimaryButtonLoading = false,
                                        uiMessage = UiMessage.ErrorMessage(it)
                                    )
                                }
                            }
                        }
                } ?: run {
                    _state.update { state ->
                        state.copy(
                            isPrimaryButtonLoading = false,
                            hasSkippedMainSeedPhrase = false
                        )
                    }
                    onRestorationComplete()
                }
            } else {
                updateSecondaryButtonLoading(true)
                args.backupType?.let { backupType ->
                    restoreProfileFromBackupUseCase(backupType = backupType, mainSeedPhraseSkipped = false)
                        .onSuccess { updateSecondaryButtonLoading(false) }
                        .onFailure { updateSecondaryButtonLoading(false) }
                }
                onRestorationComplete()
            }
        }
    }

    private suspend fun onRestorationComplete() {
        homeCardsRepository.walletRestored()
        sendEvent(Event.FinishRestoration(isMovingToMain = true))
    }

    private fun updateSecondaryButtonLoading(isLoading: Boolean) {
        _state.update { state -> state.copy(isSecondaryButtonLoading = isLoading) }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val mainBabylonFactorSourceId: FactorSourceId.Hash? = null,
        private val selectedIndex: Int = -1,
        val screenType: ScreenType = ScreenType.Loading,
        val isMovingForward: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isPrimaryButtonLoading: Boolean = false,
        val isSecondaryButtonLoading: Boolean = false,
        val hasSkippedMainSeedPhrase: Boolean = false,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState {

        sealed interface ScreenType {
            data object Loading : ScreenType
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

        val isPrimaryButtonEnabled: Boolean
            get() = (screenType != ScreenType.SeedPhrase || seedPhraseState.isValidSeedPhrase()) && !isSecondaryButtonLoading

        fun proceedToNextRecoverable() = copy(
            selectedIndex = selectedIndex + 1,
            isMovingForward = true,
            screenType = ScreenType.Entities
        )
    }

    sealed interface Event : OneOffEvent {
        data class FinishRestoration(val isMovingToMain: Boolean) : Event
        data object CloseApp : Event
    }
}

data class RecoverableFactorSource(
    val associatedAccounts: List<Account>,
    val factorSource: FactorSource.Device
) {
    val activeAccountsToDisplay: List<Account>
        get() = associatedAccounts.active()

    val areAllAccountsHidden: Boolean
        get() = associatedAccounts.isNotEmpty() && associatedAccounts.all { it.isHidden }
}
