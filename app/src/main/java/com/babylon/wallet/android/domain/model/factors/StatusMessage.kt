package com.babylon.wallet.android.domain.model.factors

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
