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
        /**
         * Represents a word that has been validated and is correct, usually displayed with a checkmark
         */
        Valid,

        /**
         * Represents a word that has been validated and is incorrect, usually displayed in red along with an error
         */
        Invalid,

        /**
         * The word has not been entered yet
         */
        Empty,

        /**
         * The word has been entered but wasn't validated yet
         */
        NotEmpty,

        /**
         * The word has been validated and is correct, but should be masked, usually with asterisks like this `****`
         */
        ValidMasked,

        /**
         * The word has been validated and is correct and cannot be edited by the user
         */
        ValidDisabled
    }
}
