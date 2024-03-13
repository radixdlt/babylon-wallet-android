package com.babylon.wallet.android.domain.model.assets

enum class SupportedCurrency(val code: String) {
    USD("USD");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code }
    }
}
