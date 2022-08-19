package com.babylon.wallet.android.presentation

import com.babylon.wallet.android.presentation.model.TokenUi
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenItemDisplayTest {

    @Test
    fun `given a token ui model, when not null symbol and not null name, then tokenItemTitle has the symbol value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = "name",
            symbol = "symbol",
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals(tokenUi.symbol, tokenUi.tokenItemTitle)
    }

    @Test
    fun `given a token ui model, when not null symbol and null name, then tokenItemTitle has the symbol value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = null,
            symbol = "symbol",
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals(tokenUi.symbol, tokenUi.tokenItemTitle)
    }

    @Test
    fun `given a token ui model, when null symbol and not null name, then tokenItemTitle has the name value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = "name",
            symbol = null,
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals(tokenUi.name, tokenUi.tokenItemTitle)
    }

    @Test
    fun `given a token ui model, when null symbol and null name, then tokenItemTitle has empty value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = null,
            symbol = null,
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals("", tokenUi.tokenItemTitle)
    }

    @Test
    fun `given a token ui model, when blank symbol and not null name or blank, then tokenItemTitle has name value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = "name",
            symbol = " ",
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals(tokenUi.name, tokenUi.tokenItemTitle)
    }

    @Test
    fun `given a token ui model, when blank symbol and empty name, then tokenItemTitle has empty value`() {
        val tokenUi = TokenUi(
            id = "id",
            name = "",
            symbol = " ",
            tokenQuantity = "token quantity",
            tokenValue = "token value",
            iconUrl = "icon url"
        )

        assertEquals("", tokenUi.tokenItemTitle)
    }
}
