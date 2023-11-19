package com.babylon.wallet.android.domain.seedphrase

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.Stateful
import com.babylon.wallet.android.presentation.common.UiState
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

class SeedPhraseVerificationDelegate(
    private val scope: CoroutineScope
) : Stateful<SeedPhraseVerificationDelegate.State>() {

    private var debounceJob: Job? = null

    fun init(seedSize: Int, blankWords: Int = 4) {
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
                blankIndices = wordsToFillIndexes
            )
        }
    }

    @Suppress("MagicNumber", "LongMethod")
    fun onWordChanged(index: Int, value: String) {
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
            val wordState = when {
                value.isEmpty() -> SeedPhraseWord.State.Empty
                else -> SeedPhraseWord.State.HasValue
            }
            _state.update { state ->
                val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                    it.copy(value = value, state = wordState)
                }).toPersistentList()
                state.copy(
                    seedPhraseWords = updatedWords,
                )
            }
        }
    }

    fun reset() {
        _state.update { State() }
    }

    data class State(
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val blankIndices: Set<Int> = emptySet()
    ) : UiState {

        val wordsPhrase: String
            get() = seedPhraseWords.joinToString(separator = " ") { it.value }

        val wordsToConfirm: Map<Int, String>
            get() = blankIndices.associateWith { seedPhraseWords[it].value }
    }

    override fun initialState(): State {
        return State()
    }
}
