package com.babylon.wallet.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.radixdlt.sargon.extensions.toDecimal192
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class FiatPriceTest {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    @Test
    fun testDefaultFormatting() {
        var fiatPrice = FiatPrice("0.0065789".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$0.01", fiatPrice.formatted())

        fiatPrice = FiatPrice("5.1265".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$5.13", fiatPrice.formatted())

        fiatPrice = FiatPrice("155.1265".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$155.13", fiatPrice.formatted())

        fiatPrice = FiatPrice("15534".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$15,534.00", fiatPrice.formatted())
    }

    @Test
    fun testSignificantDigitsPrecisionFormatting() {
        var fiatPrice = FiatPrice("0.0065789".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$0.00658", fiatPrice.formatted(3))

        fiatPrice = FiatPrice("5.1265".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$5.13", fiatPrice.formatted(3))

        fiatPrice = FiatPrice("155.1265".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$155", fiatPrice.formatted(3))

        fiatPrice = FiatPrice("15534".toDecimal192(), SupportedCurrency.USD)
        assertEquals("$15,500", fiatPrice.formatted(3))
    }
}

class DefaultLocaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Locale.setDefault(Locale.US)
                base.evaluate()
            }
        }
    }
}
