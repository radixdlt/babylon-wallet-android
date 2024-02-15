package rdx.works.core

import com.radixdlt.ret.Decimal
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

const val MAX_TOKEN_DIGITS_LENGTH = 8
private const val MILLION_DIGITS_LENGTH = 6

// We still want to display 1,000 M so we catch 10 instead of 9 to not fall into B for thousands
private const val BILLION_DIGITS_LENGTH = 9
private const val TRILLION_DIGITS_LENGTH = 12

private const val SCIENTIFIC_NOTATION_THRESHOLD = 20

/**
 * Algorithm that implements the rules of token amount display:
 *
 * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2820538382/Wallet+Token+Amount+Display+and+Copying
 */
@Suppress("MagicNumber")
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

    return if (integralPartLength > SCIENTIFIC_NOTATION_THRESHOLD) {
        // scientific notation
        val exponentNumber = integralPartLength - 1
        val wholeNumbers = this.divide(BigDecimal(10).pow(exponentNumber))
        decimalFormat.maximumFractionDigits = 3
        decimalFormat.format(wholeNumbers).plus("e$exponentNumber")
    } else if (integralPartLength > TRILLION_DIGITS_LENGTH) {
        // trillion
        val wholeNumbers = this.divide(BigDecimal(10).pow(TRILLION_DIGITS_LENGTH))
        val wholeDecimalPlaces = wholeNumbers.toBigInteger().toString().length
        decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - wholeDecimalPlaces
        decimalFormat.format(wholeNumbers).plus(" T")
    } else if (integralPartLength > BILLION_DIGITS_LENGTH) {
        // billion
        val wholeNumbers = this.divide(BigDecimal(10).pow(BILLION_DIGITS_LENGTH))
        val wholeDecimalPlaces = wholeNumbers.toBigInteger().toString().length
        decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - wholeDecimalPlaces
        decimalFormat.format(wholeNumbers).plus(" B")
    } else if (integralPartLength > MILLION_DIGITS_LENGTH) {
        // million
        val wholeNumbers = this.divide(BigDecimal(10).pow(MILLION_DIGITS_LENGTH))
        val wholeDecimalPlaces = wholeNumbers.toBigInteger().toString().length
        decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - wholeDecimalPlaces
        decimalFormat.format(wholeNumbers).plus(" M")
    } else if (integralPartLength + decimalPartLength <= MAX_TOKEN_DIGITS_LENGTH) {
        decimalFormat.minimumFractionDigits = decimalPartLength
        decimalFormat.format(this)
    } else if (integralPartLength == 0 && decimalPartLength >= MAX_TOKEN_DIGITS_LENGTH - 1) {
        val roundTokenQuantity = this.setScale(MAX_TOKEN_DIGITS_LENGTH - 1, RoundingMode.HALF_UP).stripTrailingZeros()
        if (roundTokenQuantity == BigDecimal.ZERO) {
            "0"
        } else {
            decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - 1
            decimalFormat.format(roundTokenQuantity)
        }
    } else {
        decimalFormat.maximumFractionDigits = MAX_TOKEN_DIGITS_LENGTH - integralPartLength
        decimalFormat.format(this)
    }
}

@Suppress("MagicNumber")
fun BigDecimal.toRETDecimal(roundingMode: RoundingMode): Decimal = Decimal(setScale(18, roundingMode).toPlainString())

fun BigDecimal.multiplyWithDivisibility(
    multiplicand: BigDecimal?,
    divisibility: Int?,
    roundingMode: RoundingMode = RoundingMode.HALF_DOWN
): BigDecimal = divisibility?.let {
    multiply(multiplicand).setScale(it, roundingMode)
} ?: run {
    multiply(multiplicand)
}

fun BigDecimal.divideWithDivisibility(
    divisor: BigDecimal?,
    divisibility: Int?,
    roundingMode: RoundingMode = RoundingMode.HALF_DOWN
): BigDecimal = divisibility?.let {
    divide(divisor, it, roundingMode)
} ?: run {
    divide(divisor)
}
