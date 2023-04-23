package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

data class TokenUiModel(
    val name: String? = null,
    val description: String? = null,
    val symbol: String?, // short capitalized name
    val tokenQuantity: BigDecimal, // the amount of the token held
    val iconUrl: String?,
    val address: String,
    val metadata: Map<String, String> = emptyMap(),
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

    /**
     * From here it starts the algorithm that implements the rules of token amount display:
     *
     * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2820538382/Wallet+Token+Amount+Display+and+Copying
     */
    private val integralPart: BigInteger = tokenQuantity.toBigInteger()
    private val integralPartLength = if (integralPart.signum() == 0) 0 else integralPart.toString().length
    private val tokenQuantityString = tokenQuantity.toString()
    private val decimalPartLength = if (tokenQuantityString.contains(".") &&
        (tokenQuantityString.substringAfter(".") == "0").not()
    ) {
        tokenQuantityString.substringAfter(".").length
    } else {
        0
    }

    private val decimalFormat = DecimalFormat.getInstance(Locale.getDefault())

    // the token amount to display on token card
    val tokenQuantityToDisplay: String
        get() = if (integralPartLength >= MAX_TOKEN_DIGITS_LENGTH && decimalPartLength == 0) {
            decimalFormat.format(tokenQuantity)
        } else if (integralPartLength >= MAX_TOKEN_DIGITS_LENGTH && decimalPartLength > 0) {
            decimalFormat.maximumFractionDigits = 1
            decimalFormat.format(tokenQuantity)
        } else if (integralPartLength + decimalPartLength <= MAX_TOKEN_DIGITS_LENGTH) {
            decimalFormat.minimumFractionDigits = decimalPartLength
            decimalFormat.format(tokenQuantity)
        } else if (integralPartLength == 0 && decimalPartLength >= MAX_TOKEN_DIGITS_LENGTH - 1) {
            val roundTokenQuantity = tokenQuantity.setScale(MAX_TOKEN_DIGITS_LENGTH, RoundingMode.FLOOR)
            val roundTokenQuantityStr = roundTokenQuantity.toPlainString()
            val result = roundTokenQuantityStr.toDouble()
            if (result == 0.0) {
                "0"
            } else {
                result.toString()
            }
        } else {
            decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - integralPartLength
            decimalFormat.format(tokenQuantity)
        }

    // token guaranteed amount to display on token card
    val guaranteedQuantityToDisplay: String
        get() = guaranteedQuantity?.toPlainString().orEmpty()

    fun isXrd(): Boolean {
        return symbol == MetadataConstants.SYMBOL_XRD
    }

    companion object {
        private const val MAX_TOKEN_DIGITS_LENGTH = 8
    }
}

fun List<OwnedFungibleToken>.toTokenUiModel() = map { ownedFungibleToken ->
    TokenUiModel(
        name = ownedFungibleToken.token.getTokenName(),
        symbol = ownedFungibleToken.token.getTokenSymbol(),
        tokenQuantity = ownedFungibleToken.amount,
        iconUrl = ownedFungibleToken.token.getIconUrl(),
        description = ownedFungibleToken.token.getTokenDescription(),
        metadata = ownedFungibleToken.token.getDisplayableMetadata(),
        address = ownedFungibleToken.address
    )
}

fun OwnedFungibleToken.toTokenUiModel(): TokenUiModel {
    return TokenUiModel(
        name = token.getTokenName(),
        symbol = token.getTokenSymbol(),
        tokenQuantity = amount,
        iconUrl = token.getIconUrl(),
        description = token.getTokenDescription(),
        metadata = token.getDisplayableMetadata(),
        address = token.address
    )
}
