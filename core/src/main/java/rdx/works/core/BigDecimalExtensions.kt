package rdx.works.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

private const val MAX_TOKEN_DIGITS_LENGTH = 8

/**
 * Algorithm that implements the rules of token amount display:
 *
 * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2820538382/Wallet+Token+Amount+Display+and+Copying
 */
fun BigDecimal.displayableQuantity(): String {
    val integralPart: BigInteger = this.toBigInteger()
    val integralPartLength = if (integralPart.signum() == 0) 0 else integralPart.toString().length
    val tokenQuantityString = this.toString()
    val decimalPartLength = if (tokenQuantityString.contains(".") &&
        (tokenQuantityString.substringAfter(".") == "0").not()
    ) {
        tokenQuantityString.substringAfter(".").length
    } else {
        0
    }
    val decimalFormat = DecimalFormat.getInstance(Locale.getDefault())

    return if (integralPartLength >= MAX_TOKEN_DIGITS_LENGTH && decimalPartLength == 0) {
        decimalFormat.format(this)
    } else if (integralPartLength >= MAX_TOKEN_DIGITS_LENGTH && decimalPartLength > 0) {
        decimalFormat.maximumFractionDigits = 1
        decimalFormat.format(this)
    } else if (integralPartLength + decimalPartLength <= MAX_TOKEN_DIGITS_LENGTH) {
        decimalFormat.minimumFractionDigits = decimalPartLength
        decimalFormat.format(this)
    } else if (integralPartLength == 0 && decimalPartLength >= MAX_TOKEN_DIGITS_LENGTH - 1) {
        val roundTokenQuantity = this.setScale(MAX_TOKEN_DIGITS_LENGTH, RoundingMode.FLOOR)
        val roundTokenQuantityStr = roundTokenQuantity.toPlainString()
        val result = roundTokenQuantityStr.toDouble()
        if (result == 0.0) {
            "0"
        } else {
            result.toString()
        }
    } else {
        decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - integralPartLength
        decimalFormat.format(this)
    }
}
