package com.babylon.wallet.android.data.repository.factors

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicBuilder
import com.radixdlt.sargon.MnemonicValidationOutcome
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.mapWhen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemonicBuilderClient @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    private var mnemonicBuilder = MnemonicBuilder()

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

    suspend fun isFactorAlreadyInUse(kind: FactorSourceKind): Result<Boolean> = sargonOsManager.callSafely(dispatcher) {
        isFactorSourceAlreadyInUse(mnemonicBuilder.getFactorSourceId(kind))
    }

    fun getFactorSourceId(kind: FactorSourceKind): FactorSourceId = mnemonicBuilder.getFactorSourceId(kind)

    suspend fun generateConfirmationWords(): List<SeedPhraseWord> = withContext(dispatcher) {
        val indices = mnemonicBuilder.getIndicesInMnemonicOfWordsToConfirm()
        val lastWordIndex = indices.lastIndex
        indices.mapIndexed { i, index ->
            SeedPhraseWord(
                index = index.toInt(),
                lastWord = i == lastWordIndex
            )
        }
    }

    suspend fun confirmWords(words: List<SeedPhraseWord>): MnemonicValidationOutcome = withContext(dispatcher) {
        mnemonicBuilder.validateWords(words.associate { it.index.toUShort() to it.value })
    }

    suspend fun getMnemonicWithPassphrase(): MnemonicWithPassphrase = withContext(dispatcher) {
        mnemonicBuilder.getMnemonicWithPassphrase()
    }

    suspend fun getWords(state: SeedPhraseWord.State): List<SeedPhraseWord> = withContext(dispatcher) {
        val bip39Words = mnemonicBuilder.getWords()
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

    suspend fun getExistingFactorSource(kind: FactorSourceKind): Result<FactorSource?> =
        sargonOsManager.callSafely(dispatcher) {
            val id = mnemonicBuilder.getFactorSourceId(kind)
            factorSources().firstOrNull { it.id == id }
        }

    private suspend fun executeMutating(function: suspend MnemonicBuilder.() -> MnemonicBuilder) =
        withContext(dispatcher) {
            mnemonicBuilder = mnemonicBuilder.function()
        }
}
