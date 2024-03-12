@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun BigDecimal.toRETDecimalString(): String = setScale(18, RoundingMode.HALF_UP).toPlainString()

fun BigDecimal.toLocaleNumberFormat(): String {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    return numberFormat.format(this)
}