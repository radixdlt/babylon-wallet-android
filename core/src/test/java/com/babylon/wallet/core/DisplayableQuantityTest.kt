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
        val expectedTokenQuantityToDisplay = "123,456,789,012,345.4"

        val actual = BigDecimal(123456789012345.4).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 120073000000123,92, then the token amount to display is 120,073,000,000,123-dot-9`() {
        val expectedTokenQuantityToDisplay = "120,073,000,000,123.9"

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
        val expectedTokenQuantityToDisplay = "12,345,678,901.1"

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
        val expectedTokenQuantityToDisplay = "1,234,567,890"

        val actual = BigDecimal(1234567890.01).displayableQuantity()

        Assert.assertEquals(expectedTokenQuantityToDisplay, actual)
    }

    @Test
    fun `given a token quantity of 1234567,19 then the token amount to display is 1,234,567-dot-2`() {
        val expectedTokenQuantityToDisplay = "1,234,567.2"

        val actual = BigDecimal(1234567.19).displayableQuantity()

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
