package com.babylon.wallet.android.presentation.ui

import com.babylon.wallet.android.presentation.settings.legacyimport.SeedPhraseWord
import kotlinx.collections.immutable.persistentListOf

object MockUiProvider {

    val seedPhraseWords = persistentListOf(
        SeedPhraseWord(
            index = 0,
            value = "casino",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 1,
            value = "frequent",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 2,
            value = "value2",
            state = SeedPhraseWord.State.Invalid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 3,
            value = "value3",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 0,
            value = "value4",
            state = SeedPhraseWord.State.Valid,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 0,
            value = "value5",
            state = SeedPhraseWord.State.Valid,
            lastWord = true
        )
    )
}
