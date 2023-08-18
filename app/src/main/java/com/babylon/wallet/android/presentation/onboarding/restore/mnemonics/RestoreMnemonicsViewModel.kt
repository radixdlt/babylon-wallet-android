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
import com.babylon.wallet.android.presentation.settings.backup.SeedPhraseLength
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.extensions.removeLeadingZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import rdx.works.profile.domain.deviceFactorSources
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class RestoreMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<RestoreMnemonicsViewModel.State>(),
    OneOffEventHandler<RestoreMnemonicsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = RestoreMnemonicsArgs(savedStateHandle)
    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSources = args.deviceFactorSource()?.let { listOf(it) } ?: getAllRecoverableFactorSources()
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
            _state.update { it.copy(isShowingEntities = true) }
        } else {
            viewModelScope.launch { sendEvent(Event.FinishRestoration(isMovingToMain = args.factorSourceIdHex == null)) }
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
            _state.update { it.copy(isShowingEntities = false) }
        } else {
            viewModelScope.launch { restoreMnemonic() }
        }
    }

    private suspend fun RestoreMnemonicsArgs.deviceFactorSource(): RecoverableFactorSource? {
        if (factorSourceIdHex == null) return null

        val factorSource = getProfileUseCase.deviceFactorSources.first().find { it.id.body.value == factorSourceIdHex }
        return factorSource?.toRecoverableFactorSource()
    }

    private suspend fun DeviceFactorSource.toRecoverableFactorSource(): RecoverableFactorSource? {
        val associatedAccounts = getProfileUseCase.accountsOnCurrentNetwork()
            .filter { it.factorSourceId() == id }

        val associatedPersonas = getProfileUseCase.personasOnCurrentNetwork()
            .filter { it.factorSourceId() == id }

        if (associatedAccounts.isEmpty() && associatedPersonas.isEmpty()) return null

        return RecoverableFactorSource(
            associatedAccounts = associatedAccounts,
            associatedPersonas = associatedPersonas,
            factorSource = this
        )
    }

    private suspend fun getAllRecoverableFactorSources(): List<RecoverableFactorSource> =
        getProfileUseCase.deviceFactorSources.first().filterNot {
            mnemonicRepository.readMnemonic(it.id) != null
        }.mapNotNull { deviceFactorSource ->
            deviceFactorSource.toRecoverableFactorSource()
        }

    private suspend fun restoreMnemonic() {
        val account = state.value.recoverableFactorSource?.associatedAccounts?.firstOrNull() ?: return
        val factorInstance = (account.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.transactionSigning ?: return
        val derivationPath = factorInstance.derivationPath ?: return
        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = _state.value.seedPhraseState.wordsPhrase,
            bip39Passphrase = _state.value.seedPhraseState.bip39Passphrase
        )

        val factorSourceIDFromHash = (factorInstance.factorSourceId as FactorSource.FactorSourceID.FromHash)
        val isFactorSourceIdValid = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase) ==
                factorSourceIDFromHash.body.value

        val isPublicKeyValid = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
            .removeLeadingZero()
            .toHexString() == factorInstance.publicKey.compressedData

        if (!isFactorSourceIdValid || !isPublicKeyValid) {
            _state.update { it.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic) }
        } else {
            restoreMnemonicUseCase(
                factorSourceId = factorSourceIDFromHash,
                mnemonicWithPassphrase = mnemonicWithPassphrase
            )
            appEventBus.sendEvent(AppEvent.RestoredMnemonic)

            showNextRecoverableFactorSourceOrFinish()
        }
    }

    private suspend fun showNextRecoverableFactorSourceOrFinish() {
        seedPhraseInputDelegate.reset()
        // This may need to change according to the length saved in profile
        seedPhraseInputDelegate.setSeedPhraseSize(SeedPhraseLength.TWENTY_FOUR.words)

        if (state.value.nextRecoverableFactorSource != null) {
            _state.update { it.proceedToNextRecoverable() }
        } else {
            sendEvent(Event.FinishRestoration(isMovingToMain = args.factorSourceIdHex == null))
        }
    }

    data class State(
        private val recoverableFactorSources: List<RecoverableFactorSource> = emptyList(),
        private val selectedIndex: Int = -1,
        val isShowingEntities: Boolean = true,
        val uiMessage: UiMessage? = null,
        val seedPhraseState: SeedPhraseInputDelegate.State = SeedPhraseInputDelegate.State()
    ) : UiState {

        val nextRecoverableFactorSource: RecoverableFactorSource?
            get() = recoverableFactorSources.getOrNull(selectedIndex + 1)

        val recoverableFactorSource: RecoverableFactorSource?
            get() = if (selectedIndex == -1) null else recoverableFactorSources.getOrNull(selectedIndex)

        fun proceedToNextRecoverable() = copy(
            selectedIndex = selectedIndex + 1,
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
    val associatedPersonas: List<Network.Persona>,
    val factorSource: DeviceFactorSource
)


