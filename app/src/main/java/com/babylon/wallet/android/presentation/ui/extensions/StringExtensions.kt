@file:Suppress("MagicNumber")
package com.babylon.wallet.android.presentation.ui.extensions

import java.text.DecimalFormat

fun String.truncatedHash(): String {
    val first = substring(IntRange(0, 3))
    val last = substring(IntRange(length - 9, length - 1))
    return "$first...$last"
}

fun String.formatDecimalSeparator(): String {
    if (isEmpty()) return this
    val formatter = DecimalFormat("#,###.##")
    return try {
        formatter.format(toDouble())
    } catch (e: NumberFormatException) {
        ""
    }
}
