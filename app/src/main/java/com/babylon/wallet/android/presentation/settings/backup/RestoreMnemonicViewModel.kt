package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
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
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import javax.inject.Inject

@HiltViewModel
class RestoreMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProfileUseCase: GetProfileUseCase,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<RestoreMnemonicViewModel.State>(), OneOffEventHandler<RestoreMnemonicViewModel.Effect> by OneOffEventHandlerImpl() {

    private val args = RestoreMnemonicArgs(savedStateHandle)

    override fun initialState(): State = State(
        accountAddress = args.factorSourceId,
        mnemonicWords = listOf(),
        passphrase = "",
        accountOnNetwork = null,
        acceptedSeedPhraseLength = SeedPhraseLength.TWENTY_FOUR,
        factorSourceLabel = ""
    )

    init {
        viewModelScope.launch {
            val profile = getProfileUseCase().first()
            val account = profile.currentNetwork.accounts.find { it.address == args.factorSourceId }
            val factorSourceId = FactorSource.ID(args.factorSourceId)
            val factorSource = profile.factorSources.find { it.id == factorSourceId }
            _state.update {
                it.copy(
                    accountOnNetwork = account,
                    factorSourceLabel = factorSource?.label.orEmpty()
                )
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onMnemonicWordsTyped(words: String) {
        _state.update { state ->
            state.copy(mnemonicWords = words.split("\\s+".toRegex()))
        }
    }

    fun onChangeSeedPhraseLength(length: SeedPhraseLength) {
        _state.update { it.copy(acceptedSeedPhraseLength = length) }
    }

    fun onPassphraseTyped(passphrase: String) {
        _state.update { it.copy(passphrase = passphrase) }
    }

    fun onRestore() {
        val account = _state.value.accountOnNetwork ?: return
        val factorInstance = (account.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.transactionSigning ?: return
        val derivationPath = factorInstance.derivationPath ?: return

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = _state.value.wordsPhrase,
            bip39Passphrase = _state.value.passphrase
        )

        val isFactorSourceIdValid = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase) ==
            factorInstance.factorSourceId

        val isPublicKeyValid = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
            .removeLeadingZero()
            .toHexString() == factorInstance.publicKey.compressedData

        val isValid = isFactorSourceIdValid && isPublicKeyValid
        if (!isValid) {
            _state.update { it.copy(uiMessage = UiMessage.InfoMessage.InvalidMnemonic) }
        } else {
            viewModelScope.launch {
                restoreMnemonicUseCase(factorSourceId = factorInstance.factorSourceId, mnemonicWithPassphrase = mnemonicWithPassphrase)
                appEventBus.sendEvent(AppEvent.RestoredMnemonic)
                sendEvent(Effect.FinishRestoration)
            }
        }
    }

    data class State(
        val accountAddress: String,
        private val mnemonicWords: List<String>,
        val passphrase: String,
        val accountOnNetwork: Network.Account?,
        val factorSourceLabel: String,
        val acceptedSeedPhraseLength: SeedPhraseLength,
        val uiMessage: UiMessage? = null,
    ) : UiState {

        val wordsPhrase: String
            get() = mnemonicWords.joinToString(separator = " ")

        val wordsHint: String
            get() = "${mnemonicWords.filterNot { it == "" }.size}/${acceptedSeedPhraseLength.words}"

        private val isWordCountValid: Boolean
            get() = mnemonicWords.filterNot { it == "" }.size == acceptedSeedPhraseLength.words

        val isSubmitButtonEnabled: Boolean
            get() = isWordCountValid && accountOnNetwork != null
    }

    sealed interface Effect : OneOffEvent {
        object FinishRestoration : Effect
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
