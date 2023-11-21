package com.babylon.wallet.android.presentation.common.seedphrase

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
