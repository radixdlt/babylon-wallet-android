package com.babylon.wallet.core

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import rdx.works.core.displayableQuantity
import java.math.BigDecimal
import java.util.Locale

class TokenQuantityToDisplayTest {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    @Test
    fun `given a token quantity of 1234,5678, then the token amount to display is 1,234-dot-5678`() {
        val expectedTokenQuantityToDisplay = "1,234.5678"

        val actual = BigDecimal(1234.5678).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 123456789012345,4, then the token amount to display is 123,456,789,012,345-dot-4`() {
        val expectedTokenQuantityToDisplay = "123.45679 T"

        val actual = BigDecimal(123456789012345.4).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 120073000000123,92, then the token amount to display is 120,073,000,000,123-dot-9`() {
        val expectedTokenQuantityToDisplay = "120.073 T"

        val actual = BigDecimal(120073000000123.92).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 1000,500 then the token amount to display is 1,000-dot-5`() {
        val expectedTokenQuantityToDisplay = "1,000.5"

        val actual = BigDecimal(1000.500).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of ,1234567 then the token amount to display is 0-dot-1234567`() {
        val expectedTokenQuantityToDisplay = "0.1234567"

        val actual = BigDecimal(0.1234567).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 0,005000000000002 then the token amount to display is 0-dot-005`() {
        val expectedTokenQuantityToDisplay = "0.005"

        val actual = BigDecimal(0.005000000000002).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 3959,8617984 then the token amount to display is 3,959-dot-8618`() {
        val expectedTokenQuantityToDisplay = "3,959.8618"

        val actual = BigDecimal(3959.8617984).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 12345678901,1000 then the token amount to display is 12,345,678,901-dot-1`() {
        val expectedTokenQuantityToDisplay = "12.345679 B"

        val actual = BigDecimal(12345678901.1000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 12345,12345 then the token amount to display is 12345-dot-12345`() {
        val expectedTokenQuantityToDisplay = "12,345.123"

        val actual = BigDecimal(12345.12345).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 0,001000000000001 then the token amount to display is 0-dot-001000000000001`() {
        val expectedTokenQuantityToDisplay = "0.001"

        val actual = BigDecimal(0.001000000000001).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 12345,0029999 then the token amount to display is 12,345-dot-003`() {
        val expectedTokenQuantityToDisplay = "12,345.003"

        val actual = BigDecimal(12345.0029999).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 1234567890,01 then the token amount to display is 1,234,567,890`() {
        val expectedTokenQuantityToDisplay = "1.2345679 B"

        val actual = BigDecimal(1234567890.01).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 1234567,19 then the token amount to display is 1,234,567-dot-2`() {
        val expectedTokenQuantityToDisplay = "1.2345672 M"

        val actual = BigDecimal(1234567.19).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    /**
     * Table of all examples taken from here as a source of truth ->
     * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2820538382/Wallet+Token+Amount+Display+and+Copying
     */
    @Test
    fun `test1`() {
        val expectedTokenQuantityToDisplay = "0.1234568"

        val actual = BigDecimal(0.123456789).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test2`() {
        val expectedTokenQuantityToDisplay = "0.4321"

        val actual = BigDecimal(0.4321).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test3`() {
        val expectedTokenQuantityToDisplay = "0"

        val actual = BigDecimal(0.0000000000001).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test4`() {
        val expectedTokenQuantityToDisplay = "1"

        val actual = BigDecimal(0.9999999999999).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test5`() {
        val expectedTokenQuantityToDisplay = "1,000"

        val actual = BigDecimal(1000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test6`() {
        val expectedTokenQuantityToDisplay = "1,000.01"

        val actual = BigDecimal(1000.01).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test7`() {
        val expectedTokenQuantityToDisplay = "1,000.1235"

        val actual = BigDecimal(1000.123456789).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test8`() {
        val expectedTokenQuantityToDisplay = "1.0000001 M"

        val actual = BigDecimal(1000000.1234).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test9`() {
        val expectedTokenQuantityToDisplay = "10 M"

        val actual = BigDecimal(10000000.1234).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test9a`() {
        val expectedTokenQuantityToDisplay = "10.000001 M"

        val actual = BigDecimal(10000001.1234).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test10`() {
        val expectedTokenQuantityToDisplay = "100.12346 M"

        val actual = BigDecimal(100123456).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test11`() {
        val expectedTokenQuantityToDisplay = "1.0001235 B"

        val actual = BigDecimal(1000123456).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test12`() {
        val expectedTokenQuantityToDisplay = "100.00012 B"

        val actual = BigDecimal(100000123456).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `test13`() {
        val expectedTokenQuantityToDisplay = "100.00012 T"

        val actual = BigDecimal(100000123456789).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    /**
     * Source -> https://rdxworks.slack.com/archives/C03QFAWBRNX/p1695290073987009?thread_ts=1695198620.735639&cid=C03QFAWBRNX
     */

    @Test
    fun gustafTableExample() {
        val expectedTokenQuantityToDisplay = "1.234567 M"

        val actual = BigDecimal(1234567).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample1() {
        val expectedTokenQuantityToDisplay = "1 M"

        val actual = BigDecimal(1000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample2() {
        val expectedTokenQuantityToDisplay = "10 M"

        val actual = BigDecimal(10000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample3() {
        val expectedTokenQuantityToDisplay = "100 M"

        val actual = BigDecimal(100000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample4() {
        val expectedTokenQuantityToDisplay = "1 B"

        val actual = BigDecimal(1000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample5() {
        val expectedTokenQuantityToDisplay = "10 B"

        val actual = BigDecimal(10000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample6() {
        val expectedTokenQuantityToDisplay = "100 B"

        val actual = BigDecimal(100000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample7() {
        val expectedTokenQuantityToDisplay = "1 T"

        val actual = BigDecimal(1000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample8() {
        val expectedTokenQuantityToDisplay = "10 T"

        val actual = BigDecimal(10000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample9() {
        val expectedTokenQuantityToDisplay = "100 T"

        val actual = BigDecimal(100000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample10() {
        val expectedTokenQuantityToDisplay = "1,000 T"

        val actual = BigDecimal(1000000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample11() {
        val expectedTokenQuantityToDisplay = "10,000 T"

        val actual = BigDecimal(10000000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample12() {
        val expectedTokenQuantityToDisplay = "100,000 T"

        val actual = BigDecimal(100000000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun gustafTableExample13() {
        val expectedTokenQuantityToDisplay = "1,000,000 T"

        val actual = BigDecimal(1000000000000000000).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `scientific notation close to 0`() {
        val expectedTokenQuantityToDisplay = "0"

        val actual = BigDecimal("0.8616E-14").displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }
}

class DefaultLocaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Locale.setDefault(Locale.UK)
                base.evaluate()
            }
        }
    }
}
