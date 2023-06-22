package com.babylon.wallet.android.domain.model

data class Tag(
    val name: String,
    val isXrd: Boolean
) {
    companion object {
        val OfficialRadix = Tag(name = "Official Radix", isXrd = true)
    }
}
