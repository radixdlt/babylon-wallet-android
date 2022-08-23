@file:Suppress("MagicNumber")
package com.babylon.wallet.android.utils

import java.text.DecimalFormat

fun String.truncatedHash(): String {
    val first = substring(IntRange(0, 3))
    val last = substring(IntRange(length - 9, length - 1))
    return "$first...$last"
}

fun String.formatDecimalSeparator(): String {
    if (isEmpty()) return this
    val formatter = DecimalFormat("#,###.##")
    val amount = replace(",", "")
    return try {
        formatter.format(amount.toDouble())
    } catch (e: NumberFormatException) {
        ""
    }
}
