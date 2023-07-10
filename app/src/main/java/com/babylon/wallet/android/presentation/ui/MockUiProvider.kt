package com.babylon.wallet.android.presentation.ui

import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate.SeedPhraseWord
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
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 4,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 5,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = true
        ),
        SeedPhraseWord(
            index = 6,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 7,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = false
        ),
        SeedPhraseWord(
            index = 8,
            value = "",
            state = SeedPhraseWord.State.Empty,
            lastWord = true
        )
    )
}
