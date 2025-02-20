package com.babylon.wallet.android.presentation.common.seedphrase

data class SeedPhraseWord(
    val index: Int,
    val value: String = "",
    val state: State = State.Empty,
    val lastWord: Boolean = false
) {

    val valid: Boolean
        get() = state == State.Valid || state == State.ValidMasked || state == State.ValidDisabled

    val inputDisabled: Boolean
        get() = state == State.ValidMasked || state == State.ValidDisabled

    val masked: Boolean
        get() = state == State.ValidMasked

    val hasValue
        get() = state == State.NotEmpty || state == State.ValidMasked || state == State.ValidDisabled

    enum class State {
        Valid, Invalid, Empty, NotEmpty, ValidMasked, ValidDisabled
    }
}
