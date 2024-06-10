package com.babylon.wallet.core

import com.radixdlt.sargon.extensions.hexToBagOfBytes
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import rdx.works.core.decodeHex
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.generateX25519KeyPair
import rdx.works.core.generateX25519SharedSecret
import rdx.works.core.toByteArray
import rdx.works.core.toHexString
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

class EncryptionHelperTest {

    private val encryptedMessageInHex =
        "6ea80ead36e3fc4f1ad75134776c26534e73086e93f6b3cd7fdbbe390ed428b5c2f0150fd3f16c928e968497060b39ec61660704"
    private val expectedDecryptedMessage = "Hello Android from Swift"

    @Test
    fun `decrypt with AES GCM NoPadding`() {
        val expectedEncryptionKeyHex = "abababababababababababababababababababababababababababababababab"

        val encryptionKeyData = getEncryptionKeyData()

        val encryptionKeyHex = encryptionKeyData.toByteArray().toHexString()

        Assert.assertEquals(expectedEncryptionKeyHex, encryptionKeyHex)

        val encryptionKeyByteArray = encryptionKeyData.array()

        val actualDecryptedMessage = encryptedMessageInHex.hexToBagOfBytes().toByteArray().decrypt(
            withEncryptionKey = encryptionKeyByteArray
        )

        Assert.assertEquals(expectedDecryptedMessage, String(actualDecryptedMessage.getOrThrow(), UTF_8))
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

    @Test
    fun `test x25519 key exchange and using shared secret with AES`() {
        val testString = "Hello Android"
        val x25519TestVectors = generateX25519TestVectors()

        val testVectors = Json.decodeFromString<List<X25519TestVector>>(x25519TestVectors)

        testVectors.forEach {
            val sharedSecret1 = generateX25519SharedSecret(
                it.privateKey1.decodeHex(),
                it.publicKey2.decodeHex()
            ).getOrThrow()

            val sharedSecret2 = generateX25519SharedSecret(
                it.privateKey2.decodeHex(),
                it.publicKey1.decodeHex()
            ).getOrThrow()

            Assert.assertEquals(it.sharedSecret, sharedSecret1)
            Assert.assertEquals(it.sharedSecret, sharedSecret2)
            val encryptedTestString = testString.toByteArray().encrypt(
                withEncryptionKey = sharedSecret1.decodeHex()
            ).getOrThrow().toHexString()

            val decryptedTestString = encryptedTestString.decodeHex().decrypt(
                withEncryptionKey = sharedSecret2.decodeHex()
            ).getOrThrow().toString(UTF_8)
            Assert.assertEquals(testString, decryptedTestString)
        }
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

    private fun generateX25519TestVectors(): String {
        val result = mutableListOf<X25519TestVector>()
        repeat(10) {
            val kp1 = generateX25519KeyPair().getOrThrow()
            val kp2 = generateX25519KeyPair().getOrThrow()
            val secret1 = generateX25519SharedSecret(kp1.first.decodeHex(), kp2.second.decodeHex()).getOrThrow()
            val secret2 = generateX25519SharedSecret(kp2.first.decodeHex(), kp1.second.decodeHex()).getOrThrow()
            if (secret1 != secret2) {
                throw IllegalStateException("Secrets do not match")
            }
            result.add(
                X25519TestVector(
                    kp1.first,
                    kp1.second,
                    kp2.first,
                    kp2.second,
                    secret1
                )
            )
        }
        return Json.encodeToString(result)
    }

    @Serializable
    data class X25519TestVector(
        val privateKey1: String,
        val publicKey1: String,
        val privateKey2: String,
        val publicKey2: String,
        val sharedSecret: String
    )
}
