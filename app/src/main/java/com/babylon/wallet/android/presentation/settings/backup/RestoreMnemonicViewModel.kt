package com.babylon.wallet.android.presentation.settings.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.InfoMessageType
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.toExtendedKey
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.backup.RestoreMnemonicUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RestoreMnemonicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProfileUseCase: GetProfileUseCase,
    private val restoreMnemonicUseCase: RestoreMnemonicUseCase
): StateViewModel<RestoreMnemonicViewModel.State>() {

    private val args = RestoreMnemonicArgs(savedStateHandle)

    override fun initialState(): State = State(
        accountAddress = args.accountAddress,
        mnemonicWords = "",
        passphrase = "",
        accountOnNetwork = null,
        isWordCountValid = false
    )

    init {
        viewModelScope.launch {
            val account = getProfileUseCase.accountsOnCurrentNetwork.first().find { it.address == args.accountAddress }
            _state.update { it.copy(accountOnNetwork = account) }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onMnemonicWordsTyped(words: String) {
        val count = words.trim().split(" ").size
        _state.update {
            it.copy(
                mnemonicWords = words,
                isWordCountValid = validWordCounts.contains(count)
            )
        }
    }

    fun onPassphraseTyped(passphrase: String) {
        _state.update { it.copy(passphrase = passphrase) }
    }

    fun onRestore() {
        val account = _state.value.accountOnNetwork ?: return
        val factorInstance = (account.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.genesisFactorInstance ?: return
        val derivationPath = factorInstance.derivationPath ?: return

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = _state.value.mnemonicWords,
            bip39Passphrase = _state.value.passphrase
        )

        val isFactorSourceIdValid = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase) ==
                factorInstance.factorSourceId

        val isPublicKeyValid = mnemonicWithPassphrase.compressedPublicKey(derivationPath = derivationPath)
            .removeLeadingZero()
            .toHexString() == factorInstance.publicKey.compressedData

        val isValid = isFactorSourceIdValid && isPublicKeyValid
        if (!isValid) {
            _state.update { it.copy(uiMessage = UiMessage.InfoMessage(type = InfoMessageType.InvalidMnemonic)) }
        } else {
            viewModelScope.launch {
                Timber.d("Restoring mnemonic for ${factorInstance.factorSourceId} with $mnemonicWithPassphrase")
                //restoreMnemonicUseCase(factorSourceId = factorInstance.factorSourceId, mnemonicWithPassphrase = mnemonicWithPassphrase)
            }
        }
    }

    private fun isMnemonicValid(accountOnNetwork: Network.Account): Boolean {
        val factorInstance = (accountOnNetwork.securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.genesisFactorInstance
            ?: return false
        val derivationPath = factorInstance.derivationPath ?: return false

        val mnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = _state.value.mnemonicWords,
            bip39Passphrase = _state.value.passphrase
        )

        val publicKey = mnemonicWithPassphrase.toExtendedKey(derivationPath = derivationPath).keyPair.getCompressedPublicKey()
        val isFactorSourceIdValid = publicKey.hashToFactorId() == factorInstance.factorSourceId.value
        val isPublicKeyValid = publicKey.removeLeadingZero().toHexString() == factorInstance.publicKey.compressedData

        return isFactorSourceIdValid && isPublicKeyValid
    }

    data class State(
        val accountAddress: String,
        val mnemonicWords: String,
        val passphrase: String,
        val accountOnNetwork: Network.Account?,
        private val isWordCountValid: Boolean,
        val uiMessage: UiMessage? = null,
    ): UiState {

        val isSubmitButtonEnabled: Boolean
            get() = isWordCountValid && accountOnNetwork != null
    }

    private companion object {
        private val validWordCounts = setOf(12, 18, 24)
    }
}
