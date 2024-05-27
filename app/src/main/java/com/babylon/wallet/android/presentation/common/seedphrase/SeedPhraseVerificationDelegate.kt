package com.babylon.wallet.android.presentation.common.seedphrase

import com.babylon.wallet.android.presentation.common.Stateful
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Bip39Language
import com.radixdlt.sargon.Bip39Word
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
                value = randomEnglishWord().word,
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
            delay(DEBOUNCE_DELAY_MS)
            val wordState = when {
                value.isEmpty() -> SeedPhraseWord.State.Empty
                else -> SeedPhraseWord.State.HasValue
            }
            _state.update { state ->
                val updatedWords = state.seedPhraseWords.mapWhen(predicate = { it.index == index }, mutation = {
                    it.copy(value = value.trim(), state = wordState)
                }).toPersistentList()
                state.copy(
                    seedPhraseWords = updatedWords,
                )
            }
        }
    }

    private fun randomEnglishWord(): Bip39Word {
        val wordList = Bip39Language.ENGLISH.wordList
        return wordList[Random.nextInt(wordList.size)]
    }

    data class State(
        val seedPhraseWords: ImmutableList<SeedPhraseWord> = persistentListOf(),
        val blankIndices: Set<Int> = emptySet()
    ) : UiState {

        val wordsToConfirm: Map<Int, String>
            get() = blankIndices.associateWith { seedPhraseWords[it].value }

        val allFieldsHaveValue: Boolean
            get() = seedPhraseWords.all { it.state == SeedPhraseWord.State.HasValue || it.state == SeedPhraseWord.State.ValidDisabled }
    }

    override fun initialState(): State {
        return State()
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 75L
    }
}
