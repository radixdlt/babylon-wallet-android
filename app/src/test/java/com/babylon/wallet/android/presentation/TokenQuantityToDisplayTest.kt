package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.model.TokenUiModel
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class TokenQuantityToDisplayTest {

    @Test
    fun `given a token quantity of 1234,5678, then the token amount to display is 1,234-dot-5678`() {
        val expectedTokenQuantityToDisplay = "1,234.5678"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(1234.5678),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 123456789012345,4, then the token amount to display is 123,456,789,012,345-dot-4`() {
        val expectedTokenQuantityToDisplay = "123,456,789,012,345.4"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(123456789012345.4),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 120073000000123,92, then the token amount to display is 120,073,000,000,123-dot-9`() {
        val expectedTokenQuantityToDisplay = "120,073,000,000,123.9"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(120073000000123.92),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 1000,500 then the token amount to display is 1,000-dot-5`() {
        val expectedTokenQuantityToDisplay = "1,000.5"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(1000.500),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of ,1234567 then the token amount to display is 0-dot-1234567`() {
        val expectedTokenQuantityToDisplay = "0.1234567"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(0.1234567),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 0,005000000000002 then the token amount to display is 0-dot-005`() {
        val expectedTokenQuantityToDisplay = "0.005"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(0.005000000000002),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 3959,8617984 then the token amount to display is 3,959-dot-8618`() {
        val expectedTokenQuantityToDisplay = "3,959.8618"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(3959.8617984),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 12345678901,1000 then the token amount to display is 12,345,678,901-dot-1`() {
        val expectedTokenQuantityToDisplay = "12,345,678,901.1"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(12345678901.1000),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 12345,12345 then the token amount to display is 12345-dot-12345`() {
        val expectedTokenQuantityToDisplay = "12,345.123"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(12345.12345),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 0,001000000000001 then the token amount to display is 0-dot-001000000000001`() {
        val expectedTokenQuantityToDisplay = "0.001"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(0.001000000000001),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 12345,0029999 then the token amount to display is 12,345-dot-003`() {
        val expectedTokenQuantityToDisplay = "12,345.003"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(12345.0029999),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 1234567890,01 then the token amount to display is 1,234,567,890`() {
        val expectedTokenQuantityToDisplay = "1,234,567,890"

        val tokenUi = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(1234567890.01),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUi.tokenQuantityToDisplay)
    }

    @Test
    fun `given a token quantity of 1234567,19 then the token amount to display is 1,234,567-dot-2`() {
        val expectedTokenQuantityToDisplay = "1,234,567.2"

        val tokenUiModel = TokenUiModel(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = BigDecimal(1234567.19),
            tokenValue = "token value",
            iconUrl = "icon url",
            description = null
        )

        Assert.assertEquals(expectedTokenQuantityToDisplay, tokenUiModel.tokenQuantityToDisplay)
    }
}
