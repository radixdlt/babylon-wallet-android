@file:Suppress("MagicNumber")

package com.babylon.wallet.android.utils

import com.radixdlt.ret.Decimal
import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toRETDecimal(): Decimal = Decimal(toRETDecimalString())

fun BigDecimal.toRETDecimalString(): String = setScale(18, RoundingMode.HALF_UP).toPlainString()
