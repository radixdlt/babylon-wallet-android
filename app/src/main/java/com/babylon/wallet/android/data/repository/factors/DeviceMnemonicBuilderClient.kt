package com.babylon.wallet.android.data.repository.factors

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DeviceMnemonicBuildOutcome
import com.radixdlt.sargon.DeviceMnemonicBuilder
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.mapWhen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceMnemonicBuilderClient @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private var deviceMnemonicBuilder = DeviceMnemonicBuilder()

    suspend fun generateMnemonicWords(): List<SeedPhraseWord> = withContext(dispatcher) {
        executeMutating { generateNewMnemonic() }
        getWords(SeedPhraseWord.State.ValidDisabled)
    }

    suspend fun createMnemonicFromWords(words: List<SeedPhraseWord>): List<SeedPhraseWord> = withContext(dispatcher) {
        runCatching {
            executeMutating { createMnemonicFromWords(words.map { it.value }) }
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

    suspend fun isFactorAlreadyInUse(): Result<Boolean> = sargonOsManager.callSafely(dispatcher) {
        isFactorSourceAlreadyInUse(deviceMnemonicBuilder.getFactorSourceId())
    }

    suspend fun generateConfirmationWords(): List<SeedPhraseWord> = withContext(dispatcher) {
        val indices = deviceMnemonicBuilder.getIndicesInMnemonicOfWordsToConfirm()
        val lastWordIndex = indices.lastIndex
        indices.mapIndexed { i, index ->
            SeedPhraseWord(
                index = index.toInt(),
                lastWord = i == lastWordIndex
            )
        }
    }

    suspend fun confirmWords(words: List<SeedPhraseWord>): DeviceMnemonicBuildOutcome = withContext(dispatcher) {
        deviceMnemonicBuilder.build(words.associate { it.index.toUShort() to it.value })
    }

    suspend fun getWords(state: SeedPhraseWord.State): List<SeedPhraseWord> = withContext(dispatcher) {
        val bip39Words = deviceMnemonicBuilder.getWords()
        val lastWordIndex = bip39Words.lastIndex
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
