package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.presentation.settings.legacyimport.SeedPhraseWord
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
import timber.log.Timber

class SeedPhraseInputDelegate(
    private val scope: CoroutineScope
) : Stateful<SeedPhraseInputDelegate.State>() {

    private var debounceJob: Job? = null

    fun setSeedPhraseSize(size: Int) {
        Timber.d("Seed phrase: init")
        _state.update { state ->
            state.copy(
                seedPhraseWords = (0 until size).map {
                    SeedPhraseWord(
                        it,
                        lastWord = it == size
                    )
                }.toPersistentList()
            )
        }
    }

    @Suppress("MagicNumber")
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
                    seedPhraseValid = updatedWords.all { it.state == SeedPhraseWord.State.Valid },
                    wordAutocompleteCandidates = wordCandidates.toPersistentList()
                )
            }
            if (shouldMoveToNextWord) {
                _state.update {
                    it.copy(wordAutocompleteCandidates = persistentListOf())
                }
                onMoveToNextWord()
            }
        }
    }

    fun onPassphraseChanged(value: String) {
        _state.update { state ->
            state.copy(bip39Passphrase = value)
        }
        validateMnemonic()
    }

    private fun validateMnemonic() {
        _state.update { state ->
            state.copy(seedPhraseValid = _state.value.seedPhraseWords.all { it.state == SeedPhraseWord.State.Valid })
        }
    }

    data class State(
        val seedPhraseValid: Boolean = true,
        val bip39Passphrase: String = "",
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val wordAutocompleteCandidates: ImmutableList<String> = persistentListOf()
    ) : UiState

    override fun initialState(): State {
        return State()
    }
}

data class SeedPhraseWord(
    val index: Int,
    val value: String = "",
    val state: State = State.Empty,
    val lastWord: Boolean = false
) {
    enum class State {
        Valid, Invalid, Empty, HasValue
    }
}
