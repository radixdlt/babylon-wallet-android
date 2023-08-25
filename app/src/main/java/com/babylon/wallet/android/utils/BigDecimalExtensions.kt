@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toRETDecimalString(): String = setScale(18, RoundingMode.HALF_UP).toPlainString()
