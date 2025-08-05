package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.hasHiddenEntities
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
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
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.AddOlympiaFactorSourceUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
class ImportSingleMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addOlympiaFactorSourceUseCase: AddOlympiaFactorSourceUseCase,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    private val appEventBus: AppEventBus,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<ImportSingleMnemonicViewModel.State>(),
    OneOffEventHandler<ImportSingleMnemonicViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AddSingleMnemonicNavArgs(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State(mnemonicType = args.mnemonicType)

    init {
        seedPhraseInputDelegate.setSeedPhraseSize(
            size = when (args.mnemonicType) {
                MnemonicType.BabylonMain -> Bip39WordCount.TWENTY_FOUR
                MnemonicType.Babylon -> Bip39WordCount.TWENTY_FOUR
                MnemonicType.Olympia -> Bip39WordCount.TWELVE
            }
        )
        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { it.copy(seedPhraseState = delegateState) }
            }
        }
        initFactorSource()
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

    fun onSeedPhraseLengthChanged(value: Bip39WordCount) {
        seedPhraseInputDelegate.setSeedPhraseSize(value)
    }

    fun onAddFactorSource() {
        viewModelScope.launch {
            if (!biometricsAuthenticateUseCase()) {
                return@launch
            }

            val mnemonic = _state.value.seedPhraseState.toMnemonicWithPassphrase()
            when (args.mnemonicType) {
                MnemonicType.Babylon -> {
                    ensureBabylonFactorSourceExistUseCase.addBabylonFactorSource(mnemonic).onSuccess {
                        sendEvent(Event.FactorSourceAdded)
                    }.onFailure { error ->
                        if (error is ProfileException.SecureStorageAccess) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        } else {
                            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                        }
                    }
                }

                MnemonicType.Olympia -> {
                    addOlympiaFactorSourceUseCase(mnemonic).onSuccess {
                        sendEvent(Event.FactorSourceAdded)
                    }.onFailure { error ->
                        if (error is ProfileException.SecureStorageAccess) {
                            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                        }
                        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
                    }
                }

                MnemonicType.BabylonMain -> {}
            }
        }
    }

    fun onAddMainSeedPhrase() {
        viewModelScope.launch {
            accessFactorSourcesProxy.setTempMnemonicWithPassphrase(
                mnemonicWithPassphrase = state.value.seedPhraseState.toMnemonicWithPassphrase()
            )
            sendEvent(Event.MainSeedPhraseCompleted)
        }
    }

    private fun initFactorSource() {
        viewModelScope.launch {
            val factorSource = args.factorSourceId?.let { factorSourceId ->
                getProfileUseCase.invoke().factorSourceById(factorSourceId)
            } ?: return@launch

            val linkedEntities = sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
                entitiesLinkedToFactorSource(
                    factorSource = factorSource,
                    profileToCheck = ProfileToCheck.Current
                )
            }.getOrNull()

            _state.update {
                it.copy(
                    factorSourceCard = factorSource.toFactorSourceCard(
                        includeLastUsedOn = true,
                        accounts = linkedEntities?.accounts.orEmpty().toPersistentList(),
                        personas = linkedEntities?.personas.orEmpty().toPersistentList(),
                        hasHiddenEntities = linkedEntities.hasHiddenEntities
                    )
                )
            }
        }
    }

    data class State(
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State(),
        val mnemonicType: MnemonicType = MnemonicType.Babylon,
        val factorSourceCard: FactorSourceCard? = null,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object FactorSourceAdded : Event
        data object MainSeedPhraseCompleted : Event
    }
}
