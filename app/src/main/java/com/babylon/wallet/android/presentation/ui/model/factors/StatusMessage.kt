package com.babylon.wallet.android.presentation.ui.model.factors

data class StatusMessage(
    val message: String,
    val type: Type
) {

    enum class Type {
        SUCCESS,
        WARNING,
        ERROR
    }
}
