package com.babylon.wallet.android.data.repository.factors

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DeviceMnemonicBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.mapWhen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceFactorSourceAddingClient @Inject constructor(
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private var deviceMnemonicBuilder = DeviceMnemonicBuilder()

    suspend fun generateMnemonicWords(): List<SeedPhraseWord> = withContext(dispatcher) {
        executeMutating { generateNewMnemonicWithPassphrase() }
        getWords(SeedPhraseWord.State.ValidMasked)
    }

    suspend fun createMnemonicFromWords(words: List<SeedPhraseWord>): List<SeedPhraseWord> = withContext(dispatcher) {
        runCatching {
            executeMutating { createMnemonicWithPassphraseFromWords(words.map { it.value }) }
        }.fold(
            onSuccess = { getWords(SeedPhraseWord.State.Valid) },
            onFailure = { throwable ->
                val invalidIndices = (throwable as? CommonException.InvalidMnemonicWords)?.indicesInMnemonic?.map {
                    it.toInt()
                } ?: List(words.size) { index -> index }
                words.mapWhen(
                    predicate = { it.index in invalidIndices },
                    mutation = { it.copy(state = SeedPhraseWord.State.Invalid) }
                )
            }
        )
    }

    private suspend fun getWords(state: SeedPhraseWord.State): List<SeedPhraseWord> = withContext(dispatcher) {
        val bip39Words = deviceMnemonicBuilder.getWords()
        val lastWordIndex = bip39Words.size - 1
        bip39Words.mapIndexed { index, bip39Word ->
            SeedPhraseWord(
                index = index,
                value = bip39Word.word,
                state = state,
                lastWord = index == lastWordIndex
            )
        }
    }

    private suspend fun executeMutating(function: suspend DeviceMnemonicBuilder.() -> DeviceMnemonicBuilder) = withContext(dispatcher) {
        deviceMnemonicBuilder = deviceMnemonicBuilder.function()
    }
}