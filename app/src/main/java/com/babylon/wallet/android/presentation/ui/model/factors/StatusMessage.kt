package com.babylon.wallet.android.presentation.ui.model.factors

import androidx.compose.ui.text.AnnotatedString

data class StatusMessage(
    val message: AnnotatedString,
    val type: Type
) {

    constructor(message: String, type: Type) : this(AnnotatedString(message), type)

    enum class Type {
        SUCCESS,
        WARNING,
        ERROR
    }
}
