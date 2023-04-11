package com.babylon.wallet.android.domain

import org.junit.Assert
import org.junit.Test

internal class OlympiaWalletExportFormatTest {

    @Test
    fun `payload properly parsed into data`() {
        val parsedData = olympiaTestPayloadChunks2.parseOlympiaWalletAccountData()
        assert(parsedData?.mnemonicWordCount == 15)
        assert(parsedData?.accountData?.size == 50)
    }

    @Test
    fun `incomplete payload parsing return null`() {
        val parsedData = olympiaTestPayloadChunks2.filterIndexed { index, s -> index == 10 }.parseOlympiaWalletAccountData()
        Assert.assertNull(parsedData)
    }



}