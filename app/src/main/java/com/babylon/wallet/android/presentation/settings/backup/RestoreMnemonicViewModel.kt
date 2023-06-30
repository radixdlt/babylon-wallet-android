package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.legacyimport.SeedPhraseWord
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.extensions.removeLeadingZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import javax.inject.Inject

@HiltViewModel
class RestoreMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<RestoreMnemonicViewModel.State>(),
    OneOffEventHandler<RestoreMnemonicViewModel.Effect> by OneOffEventHandlerImpl() {

    private val args = RestoreMnemonicArgs(savedStateHandle)
    private lateinit var deviceFactorSource: DeviceFactorSource

    override fun initialState(): State = State(
        acceptedSeedPhraseLength = SeedPhraseLength.TWENTY_FOUR,
        factorSourceLabel = ""
    )

    private val seedPhraseInputDelegate = SeedPhraseInputDelegate(viewModelScope)

    init {
        seedPhraseInputDelegate.setSeedPhraseSize(SeedPhraseLength.TWENTY_FOUR.words)
        viewModelScope.launch {
            val profile = getProfileUseCase().first()
            val factorSourceId = FactorSourceID.FromHash(
                kind = FactorSourceKind.DEVICE,
                body = FactorSource.HexCoded32Bytes(args.factorSourceId)
            )
            deviceFactorSource =
                checkNotNull(profile.factorSources.find { it.id == factorSourceId }) as DeviceFactorSource
            _state.update {
                it.copy(
                    factorSourceLabel = deviceFactorSource.hint.name
                )
            }
        }
        viewModelScope.launch {
            seedPhraseInputDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        seedPhraseValid = delegateState.seedPhraseValid,
                        bip39Passphrase = delegateState.bip39Passphrase,
                        seedPhraseWords = delegateState.seedPhraseWords,
                        wordAutocompleteCandidates = delegateState.wordAutocompleteCandidates
                    )
                }
            }
        }
    }

    fun onWordChanged(index: Int, value: String) {
        seedPhraseInputDelegate.onWordChanged(index, value) {
            sendEvent(Effect.MoveToNextWord)
        }
    }

    fun onPassphraseChanged(value: String) {
        seedPhraseInputDelegate.onPassphraseChanged(value)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onChangeSeedPhraseLength(length: SeedPhraseLength) {
        _state.update { it.copy(acceptedSeedPhraseLength = length) }
        seedPhraseInputDelegate.setSeedPhraseSize(length.words)
    }

    fun onRestore() {
        viewModelScope.launch {
            val accountOnNetwork = getProfileUseCase.accountsOnCurrentNetwork().firstOrNull {
                it.factorSourceId() == deviceFactorSource.id
            } ?: return@launch
            val factorInstance =
                (accountOnNetwork.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.transactionSigning
                    ?: return@launch
            val derivationPath = factorInstance.derivationPath ?: return@launch
            val mnemonicWithPassphrase = MnemonicWithPassphrase(
                mnemonic = _state.value.wordsPhrase,
                bip39Passphrase = _state.value.bip39Passphrase
            )

            val factorSourceIDFromHash = (factorInstance.factorSourceId as FactorSourceID.FromHash)
            val isFactorSourceIdValid = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase) ==
                    factorSourceIDFromHash.body.value

            val isPublicKeyValid = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
                .removeLeadingZero()
                .toHexString() == factorInstance.publicKey.compressedData

            val isValid = isFactorSourceIdValid && isPublicKeyValid
            if (!isValid) {
                _state.update { it.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic) }
            } else {
                viewModelScope.launch {
                    restoreMnemonicUseCase(
                        factorSourceId = factorSourceIDFromHash,
                        mnemonicWithPassphrase = mnemonicWithPassphrase
                    )
                    appEventBus.sendEvent(AppEvent.RestoredMnemonic)
                    sendEvent(Effect.FinishRestoration)
                }
            }
        }
    }

    data class State(
        val seedPhraseValid: Boolean = false,
        val bip39Passphrase: String = "",
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val wordAutocompleteCandidates: ImmutableList<String> = persistentListOf(),
        val factorSourceLabel: String,
        val acceptedSeedPhraseLength: SeedPhraseLength,
        val uiMessage: UiMessage? = null,
    ) : UiState {

        val wordsPhrase: String
            get() = seedPhraseWords.joinToString(separator = " ") { it.value }
    }

    sealed interface Effect : OneOffEvent {
        object FinishRestoration : Effect
        object MoveToNextWord : Effect
    }
}

@Suppress("MagicNumber")
enum class SeedPhraseLength(val words: Int) {
    TWELVE(12),
    FIFTEEN(15),
    EIGHTEEN(18),
    TWENTY_ONE(21),
    TWENTY_FOUR(24)
}
