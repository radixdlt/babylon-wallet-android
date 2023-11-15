package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.utils.toMnemonicWords
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import kotlin.random.Random

class SeedPhraseInputDelegate(
    private val scope: CoroutineScope
) : Stateful<SeedPhraseInputDelegate.State>() {

    private var debounceJob: Job? = null

    fun initInConfirmMode(seedSize: Int, blankWords: Int = 4) {
        val seedPhraseWords = List(seedSize) { index ->
            SeedPhraseWord(
                index,
                lastWord = index == seedSize - 1,
                value = WORDLIST_ENGLISH[Random.nextInt(WORDLIST_ENGLISH.size)],
                state = SeedPhraseWord.State.ValidDisabled
            )
        }.toPersistentList()
        // always ask for last word
        val wordsToFillIndexes = mutableSetOf<Int>().apply { add(seedSize - 1) }
        do {
            wordsToFillIndexes.add(Random.nextInt(seedSize))
        } while (wordsToFillIndexes.size < blankWords)
        _state.update { state ->
            state.copy(
                seedPhraseWords = seedPhraseWords.mapWhen(predicate = { wordsToFillIndexes.contains(it.index) }, mutation = { word ->
                    word.copy(state = SeedPhraseWord.State.Empty, value = "")
                }).toPersistentList(),
            )
        }
    }

    fun setSeedPhraseSize(size: Int) {
        _state.update { state ->
            state.copy(
                seedPhraseWords = (0 until size).map {
                    SeedPhraseWord(
                        it,
                        lastWord = it == size - 1
                    )
                }.toPersistentList()
            )
        }
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

    @Suppress("MagicNumber", "LongMethod")
    fun onWordChanged(index: Int, value: String, onMoveToNextWord: suspend () -> Unit) {
        val isDeleting = (_state.value.seedPhraseWords.firstOrNull { it.index == index }?.value?.length ?: 0) > value.length
        _state.update { state ->
            val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                it.copy(value = value)
            }).toPersistentList()
            state.copy(
                seedPhraseWords = updatedWords,
            )
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(75L)
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
            var shouldMoveToNextWord = false
            val wordCandidates = if (value.isEmpty()) {
                emptyList()
            } else {
                WORDLIST_ENGLISH.filter { it.startsWith(value) }
            }
            val newValue = if (wordCandidates.size == 1 && !isDeleting) {
                shouldMoveToNextWord = true
                wordCandidates[0]
            } else {
                value
            }
            val wordState = when {
                wordCandidates.contains(newValue) -> SeedPhraseWord.State.Valid
                newValue.isEmpty() -> SeedPhraseWord.State.Empty
                wordCandidates.isEmpty() -> SeedPhraseWord.State.Invalid
                else -> SeedPhraseWord.State.HasValue
            }
            _state.update { state ->
                val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                    it.copy(value = newValue, state = wordState)
                }).toPersistentList()
                state.copy(
                    seedPhraseWords = updatedWords,
                    wordAutocompleteCandidates = wordCandidates.toPersistentList()
                )
            }
            if (shouldMoveToNextWord) {
                _state.update {
                    it.copy(wordAutocompleteCandidates = persistentListOf())
                }
                if (index != _state.value.seedPhraseWords.lastIndex) {
                    onMoveToNextWord()
                }
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

    data class SeedPhraseWord(
        val index: Int,
        val value: String = "",
        val state: State = State.Empty,
        val lastWord: Boolean = false
    ) {

        val valid: Boolean
            get() = state == State.Valid || state == State.ValidDisabled

        val inputDisabled: Boolean
            get() = state == State.ValidDisabled

        enum class State {
            Valid, Invalid, Empty, HasValue, ValidDisabled
        }
    }

    data class State(
        val bip39Passphrase: String = "",
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val wordAutocompleteCandidates: ImmutableList<String> = persistentListOf(),
        val blankIndices: Set<Int> = emptySet()
    ) : UiState {

        val seedPhraseValid: Boolean
            get() = seedPhraseWords.all { it.valid }

        val wordsPhrase: String
            get() = seedPhraseWords.joinToString(separator = " ") { it.value }

        val wordsToConfirm: Map<Int, String>
            get() = blankIndices.associateWith { seedPhraseWords[it].value }
    }

    override fun initialState(): State {
        return State()
    }
}
