package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MetadataConstants
import java.math.BigDecimal

data class TransactionTokenUiModel(
    val symbol: String?,
    val tokenQuantity: BigDecimal,
    val iconUrl: String?,
    val address: String,
    val isTokenAmountVisible: Boolean = true,
    val guaranteedQuantity: BigDecimal? = null
) : AssetUiModel() {

    fun isXrd(): Boolean {
        return symbol == MetadataConstants.SYMBOL_XRD
    }

    val tokenQuantityToDisplay: String
        get() = tokenQuantity.toPlainString().orEmpty()

    // token guaranteed amount to display on token card
    val guaranteedQuantityToDisplay: String
        get() = guaranteedQuantity?.toPlainString().orEmpty()
}
