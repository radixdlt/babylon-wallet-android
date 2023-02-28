package com.babylon.wallet.core

import org.junit.Assert
import org.junit.Test
import rdx.works.core.decryptData
import rdx.works.core.encrypt
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

class EncryptionHelperTest {

    private val encryptedMessageInHex = "6ea80ead36e3fc4f1ad75134776c26534e73086e93f6b3cd7fdbbe390ed428b5c2f0150fd3f16c928e968497060b39ec61660704"
    private val expectedDecryptedMessage = "Hello Android from Swift"

    @Test
    fun `decrypt with AES GCM NoPadding`() {
        val expectedEncryptionKeyHex = "abababababababababababababababababababababababababababababababab"

        val encryptionKeyData = getEncryptionKeyData()

        val encryptionKeyHex = encryptionKeyData.toByteArray().toHexString()

        Assert.assertEquals(expectedEncryptionKeyHex, encryptionKeyHex)

        val encryptionKeyByteArray = encryptionKeyData.array()

        val actualDecryptedMessage = encryptedMessageInHex.decodeHex().decryptData(
            withEncryptionKey = encryptionKeyByteArray
        )

        Assert.assertEquals(expectedDecryptedMessage, String(actualDecryptedMessage, UTF_8))
    }

    @Test
    fun `ensure that encrypting the same message does not give the same encrypted output`() {
        val encryptionKeyData = getEncryptionKeyData()
        val encryptionKeyByteArray = encryptionKeyData.array()

        val encryptedMessage1 = expectedDecryptedMessage.toByteArray().encrypt(
            withEncryptionKey = encryptionKeyByteArray
        )

        val encryptedMessage2 = expectedDecryptedMessage.toByteArray().encrypt(
            withEncryptionKey = encryptionKeyByteArray
        )

        Assert.assertNotEquals(encryptedMessage1, encryptedMessage2)
    }

    private fun getEncryptionKeyData(): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(32)
        for (i in 0..31) {
            byteBuffer.put(i, 0xab.toByte())
        }
        return byteBuffer
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val copy = ByteArray(remaining())
        get(copy)
        return copy
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}
