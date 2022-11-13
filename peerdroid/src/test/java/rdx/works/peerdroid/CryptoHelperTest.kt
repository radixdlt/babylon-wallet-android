package rdx.works.peerdroid

import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.junit.Assert
import org.junit.Test
import rdx.works.peerdroid.helpers.decryptWithAes
import rdx.works.peerdroid.helpers.encryptWithAes
import java.nio.ByteBuffer

class CryptoHelperTest {

    private val encryptedMessageInHex = "6ea80ead36e3fc4f1ad75134776c26534e73086e93f6b3cd7fdbbe390ed428b5c2f0150fd3f16c928e968497060b39ec61660704"
    private val expectedDecryptedMessage = "Hello Android from Swift"

    @Test
    fun `decrypt with AES GCM NoPadding`() {
        val expectedEncryptionKeyHex = "abababababababababababababababababababababababababababababababab"

        val encryptionKeyData = getEncryptionKeyData()
        val encryptionKeyHex = encryptionKeyData.toByteString().hex()

        Assert.assertEquals(expectedEncryptionKeyHex, encryptionKeyHex)

        val encryptionKeyByteArray = encryptionKeyData.array()

        val actualDecryptedMessage = decryptWithAes(
            input = encryptedMessageInHex.decodeHex().toByteArray(),
            encryptionKey = encryptionKeyByteArray
        )

        Assert.assertEquals(expectedDecryptedMessage, actualDecryptedMessage)
    }

    @Test
    fun `ensure that encrypting the same message does not give the same encrypted output`() {
        val encryptionKeyData = getEncryptionKeyData()
        val encryptionKeyByteArray = encryptionKeyData.array()

        val encryptedMessage1 = encryptWithAes(
            input = expectedDecryptedMessage.toByteArray(),
            encryptionKey = encryptionKeyByteArray
        )

        val encryptedMessage2 = encryptWithAes(
            input = expectedDecryptedMessage.toByteArray(),
            encryptionKey = encryptionKeyByteArray
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
}
