package com.babylon.wallet.android.presentation.common.seedphrase

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.Stateful
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.toMnemonicWords
import com.radixdlt.sargon.Bip39Language
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.wordList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen

class SeedPhraseInputDelegate(
    private val scope: CoroutineScope
) : Stateful<SeedPhraseInputDelegate.State>() {

    private var debounceJob: Job? = null
    private val englishWordList: List<String> by lazy {
        Bip39Language.ENGLISH.wordList.map { it.word }
    }

    fun setSeedPhraseSize(size: Bip39WordCount) {
        _state.update { state -> state.setSeedPhraseSize(size = size) }
    }

    fun onWordSelected(index: Int, value: String) {
        _state.update { state ->
            val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                it.copy(value = value, state = SeedPhraseWord.State.Valid)
            }).toPersistentList()
            state.copy(
                seedPhraseWords = updatedWords,
                wordAutocompleteCandidates = persistentListOf()
            )
        }
    }

    @Suppress("LongMethod")
    fun onWordChanged(index: Int, value: String) {
        _state.update { state ->
            val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                it.copy(value = value.trim())
            }).toPersistentList()
            state.copy(
                seedPhraseWords = updatedWords,
            )
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            if (BuildConfig.DEBUG_MODE) {
                val pastedMnemonic = value.toMnemonicWords(state.value.seedPhraseWords.size)
                if (pastedMnemonic.isNotEmpty()) {
                    _state.update { state ->
                        state.copy(
                            seedPhraseWords = state.seedPhraseWords.mapIndexed { index, word ->
                                val wordState = if (word.state == SeedPhraseWord.State.ValidDisabled) {
                                    SeedPhraseWord.State.ValidDisabled
                                } else {
                                    SeedPhraseWord.State.Valid
                                }
                                word.copy(value = pastedMnemonic[index], state = wordState)
                            }.toPersistentList()
                        )
                    }
                    return@launch
                }
            }
            val wordCandidates = if (value.isEmpty()) {
                emptyList()
            } else {
                englishWordList.filter { it.startsWith(value) }
            }

            val wordState = when {
                wordCandidates.contains(value) -> SeedPhraseWord.State.Valid
                value.isEmpty() -> SeedPhraseWord.State.Empty
                wordCandidates.isEmpty() -> SeedPhraseWord.State.Invalid
                else -> SeedPhraseWord.State.HasValue
            }
            _state.update { state ->
                val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                    it.copy(value = value.trim(), state = wordState)
                }).toPersistentList()
                state.copy(
                    seedPhraseWords = updatedWords,
                    wordAutocompleteCandidates = if (wordCandidates.size == 1 && wordCandidates.first() == value) {
                        emptyList()
                    } else {
                        wordCandidates
                    }.toPersistentList()
                )
            }
        }
    }

    fun onPassphraseChanged(value: String) {
        _state.update { state ->
            state.copy(bip39Passphrase = value)
        }
    }

    fun reset() {
        _state.update { State() }
    }

    data class State(
        val bip39Passphrase: String = "",
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val wordAutocompleteCandidates: ImmutableList<String> = persistentListOf()
    ) : UiState {

        private val isInputEmpty: Boolean
            get() = seedPhraseWords.any { it.state == SeedPhraseWord.State.Empty }

        private val isSeedPhraseInputValid: Boolean
            get() = seedPhraseWords.all { it.valid }

        fun shouldDisplayInvalidSeedPhraseWarning(): Boolean {
            if (isInputEmpty) {
                return false
            }
            return !isValidSeedPhrase()
        }

        fun isValidSeedPhrase(): Boolean {
            if (isInputEmpty) {
                return false
            }
            return isSeedPhraseInputValid && wordsIntoMnemonic().getOrNull() != null
        }

        fun validSeedPhraseOrNull(): MnemonicWithPassphrase? {
            if (isInputEmpty || !isSeedPhraseInputValid) {
                return null
            }

            return wordsIntoMnemonic().map {
                MnemonicWithPassphrase(
                    mnemonic = it,
                    passphrase = bip39Passphrase
                )
            }.getOrNull()
        }

        fun toMnemonicWithPassphrase(): MnemonicWithPassphrase = MnemonicWithPassphrase(
            mnemonic = wordsIntoMnemonic().getOrThrow(),
            passphrase = bip39Passphrase
        )

        fun setSeedPhraseSize(size: Bip39WordCount): State = copy(
            seedPhraseWords = (0 until size.value.toInt()).map {
                SeedPhraseWord(
                    it,
                    lastWord = it == size.value.toInt() - 1
                )
            }.toPersistentList()
        )

        private fun wordsIntoMnemonic(): Result<Mnemonic> = runCatching {
            Mnemonic.init(phrase = seedPhraseWords.joinToString(separator = " ") { it.value })
        }
    }

    override fun initialState(): State {
        return State()
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 75L
    }
}
