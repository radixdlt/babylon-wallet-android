package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MetadataConstants
import java.math.BigDecimal

data class TokenUiModel(
    val name: String? = null,
    val description: String? = null,
    val symbol: String?, // short capitalized name
    val tokenQuantity: BigDecimal, // the amount of the token held
    val iconUrl: String?,
    val resourceAddress: String,
    val isTokenAmountVisible: Boolean? = null,
    val guaranteedQuantity: BigDecimal? = null
) : AssetUiModel() {

    // token guaranteed amount to display on token card
    val guaranteedQuantityToDisplay: String
        get() = guaranteedQuantity?.toPlainString().orEmpty()

    fun isXrd(): Boolean {
        return symbol == MetadataConstants.SYMBOL_XRD
    }
}
