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
    /**
     * The title to show in the token list item of the Account screen.
     *
     * It is based of the token display rule:
     *
     * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2844753933/Rules+for+Display+of+Assets+in+Account+Detail+View
     *
     */
    val tokenItemTitle: String
        get() = if (symbol?.isNotBlank() == true) {
            symbol
        } else if (name?.isNotBlank() == true) {
            name
        } else {
            ""
        }

    // token guaranteed amount to display on token card
    val guaranteedQuantityToDisplay: String
        get() = guaranteedQuantity?.toPlainString().orEmpty()

    fun isXrd(): Boolean {
        return symbol == MetadataConstants.SYMBOL_XRD
    }
}
