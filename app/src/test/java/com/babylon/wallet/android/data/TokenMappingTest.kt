package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel
import com.babylon.wallet.android.mockdata.mockAccountDtoList
import org.junit.Assert
import org.junit.Test

class TokenMappingTest {

    @Test
    fun `given a list of AssetDto with token type, when XRD is included, then mapping to list of TokenUi has XRD first`() {
        val accountDto = mockAccountDtoList[0]

        val accountUi = accountDto.toUiModel()
        val listOfTokens = accountUi.tokens

        Assert.assertEquals("XRD", listOfTokens[0].symbol)
    }

    @Test
    fun `given a list of AssetDto with token type, when token without market price is included, then mapping to list of TokenUi has the token last`() {
        val accountDto = mockAccountDtoList[0]

        val accountUi = accountDto.toUiModel()
        val listOfTokens = accountUi.tokens

        Assert.assertEquals(null, listOfTokens[listOfTokens.size - 1].tokenValue)
    }
}
