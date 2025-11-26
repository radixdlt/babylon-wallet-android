package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.factorsources.hasHiddenEntities
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.supportsOlympia
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.KeystoreManager
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.DiscardTemporaryRestoredFileForBackupUseCase
import rdx.works.profile.domain.backup.GetTemporaryRestoringProfileForBackupUseCase
import rdx.works.profile.domain.backup.ImportMnemonicUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ImportMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val getTemporaryRestoringProfileForBackupUseCase: GetTemporaryRestoringProfileForBackupUseCase,
    private val importMnemonicUseCase: ImportMnemonicUseCase,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val discardTemporaryRestoredFileForBackupUseCase: DiscardTemporaryRestoredFileForBackupUseCase,
    private val appEventBus: AppEventBus,
    private val homeCardsRepository: HomeCardsRepository,
    private val keystoreManager: KeystoreManager,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<ImportMnemonicsViewModel.State>(),
    OneOffEventHandler<ImportMnemonicsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ImportMnemonicsArgs.from(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToCheck = args.backupType?.let { backupType ->
                // Reset keyspec before importing any mnemonic, when keyspec is invalid
                keystoreManager.resetMnemonicKeySpecWhenInvalidated()

                getTemporaryRestoringProfileForBackupUseCase(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)
                    ?.let {
                        ProfileToCheck.Specific(it)
                    }
            } ?: ProfileToCheck.Current

            val recoverableFactorSources = profileToCheck.recoverableFactorSources()

            _state.update {
                it.copy(
                    recoverableFactorSources = recoverableFactorSources,
                    isLoading = false
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
        val factorSources = profile.deviceFactorSources

        return factorSources.map { factorSource ->
            val linkedEntities = sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
                entitiesLinkedToFactorSource(
                    factorSource = factorSource,
                    profileToCheck = this@recoverableFactorSources
                )
            }.getOrNull()

            RecoverableFactorSource(
                factorSource = factorSource,
                card = factorSource.toFactorSourceCard(
                    accounts = linkedEntities?.accounts.orEmpty().toPersistentList(),
                    personas = linkedEntities?.personas.orEmpty().toPersistentList(),
                    hasHiddenEntities = linkedEntities.hasHiddenEntities
                )
            )
        }
    }

    fun onBackClick() {
        val previousRecoverableFactorSource = state.value.previousRecoverableFactorSource
        if (previousRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(previousRecoverableFactorSource.factorSource.value.hint.mnemonicWordCount)

            _state.update { it.proceedToPreviousRecoverable() }
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

    fun onContinueClick() {
        viewModelScope.launch { restoreMnemonic() }
    }

    private suspend fun restoreMnemonic() {
        val factorSourceToRecover = state.value.recoverableFactorSource?.factorSource ?: return

        _state.update { it.copy(isPrimaryButtonLoading = true) }

        importMnemonicUseCase(
            factorSourceId = factorSourceToRecover.value.id.asGeneral(),
            mnemonicWithPassphrase = _state.value.seedPhraseState.toMnemonicWithPassphrase()
        ).onSuccess {
            appEventBus.sendEvent(AppEvent.FixSecurityIssue.ImportedMnemonic)
            showNextRecoverableFactorSourceOrFinish()
        }.onFailure { error ->
            if (error is ProfileException.SecureStorageAccess) {
                _state.update { it.copy(isPrimaryButtonLoading = false) }
                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
            } else {
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        isPrimaryButtonLoading = false
                    )
                }
            }
        }

        _state.update { state -> state.copy(isPrimaryButtonLoading = false) }
    }

    private suspend fun showNextRecoverableFactorSourceOrFinish() {
        val nextRecoverableFactorSource = state.value.nextRecoverableFactorSource

        if (nextRecoverableFactorSource != null) {
            seedPhraseInputDelegate.reset()
            seedPhraseInputDelegate.setSeedPhraseSize(nextRecoverableFactorSource.factorSource.value.hint.mnemonicWordCount)

            _state.update { it.proceedToNextRecoverable() }
        } else {
            updateSecondaryButtonLoading(true)

            args.backupType?.let { backupType ->
                restoreProfileFromBackupUseCase(backupType = backupType)
                    .onSuccess { updateSecondaryButtonLoading(false) }
                    .onFailure { updateSecondaryButtonLoading(false) }

                homeCardsRepository.walletRestored()
            }

            onRestorationComplete()
        }
    }

    private suspend fun onRestorationComplete() {
        sendEvent(
            Event.FinishRestoration(
                isMovingToMain = args.requestSource != ImportMnemonicsRequestSource.FactorSourceDetails
            )
        )
    }

    private fun updateSecondaryButtonLoading(isLoading: Boolean) {
        _state.update { state -> state.copy(isSecondaryButtonLoading = isLoading) }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val selectedIndex: Int = -1,
        val isLoading: Boolean = true,
        val uiMessage: UiMessage? = null,
        val isPrimaryButtonLoading: Boolean = false,
        val isSecondaryButtonLoading: Boolean = false,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState {

        val previousRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex - 1)

        val nextRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex + 1)

        val recoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex)

        val isPrimaryButtonEnabled: Boolean
            get() = seedPhraseState.isValidSeedPhrase() && !isSecondaryButtonLoading

        val isOlympia: Boolean
            get() = recoverableFactorSource?.factorSource?.supportsOlympia == true

        fun proceedToNextRecoverable() = copy(
            selectedIndex = selectedIndex + 1
        )

        fun proceedToPreviousRecoverable() = copy(
            selectedIndex = selectedIndex - 1
        )
    }

    sealed interface Event : OneOffEvent {

        data class FinishRestoration(val isMovingToMain: Boolean) : Event

        data object CloseApp : Event
    }

    data class RecoverableFactorSource(
        val factorSource: FactorSource.Device,
        val card: FactorSourceCard
    )
}
