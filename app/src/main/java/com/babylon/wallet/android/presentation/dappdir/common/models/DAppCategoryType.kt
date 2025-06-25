package com.babylon.wallet.android.presentation.dappdir.common.models

import androidx.compose.runtime.Composable

enum class DAppCategoryType(val value: String) {

    DeFi("DeFi"),
    Utility("Utility"),
    Dao("Dao"),
    NFT("NFT"),
    Meme("Meme"),
    Unknown("Other");

    companion object {

        fun from(value: String?): DAppCategoryType = entries.firstOrNull {
            it.name.equals(
                other = value,
                ignoreCase = true
            )
        } ?: Unknown
    }
}

@Composable
fun DAppCategoryType.title() = when (this) { // TODO sergiu localize
    DAppCategoryType.DeFi -> "DeFi"
    DAppCategoryType.Utility -> "Utility"
    DAppCategoryType.Dao -> "Dao"
    DAppCategoryType.NFT -> "NFT"
    DAppCategoryType.Meme -> "Meme"
    DAppCategoryType.Unknown -> "Other"
}
