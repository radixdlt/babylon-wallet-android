package com.babylon.wallet.android.presentation.dappdir.common.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R

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
fun DAppCategoryType.title() = stringResource(
    id = when (this) {
        DAppCategoryType.DeFi -> R.string.dappDirectory_categoryDefi_title
        DAppCategoryType.Utility -> R.string.dappDirectory_categoryUtility_title
        DAppCategoryType.Dao -> R.string.dappDirectory_categoryDao_title
        DAppCategoryType.NFT -> R.string.dappDirectory_categoryNFT_title
        DAppCategoryType.Meme -> R.string.dappDirectory_categoryMeme_title
        DAppCategoryType.Unknown -> R.string.dappDirectory_categoryOther_title
    }
)
