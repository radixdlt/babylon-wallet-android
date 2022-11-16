package rdx.works.peerdroid.helpers

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_ALGORITHM = "AES"
private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

private const val IV_BYTE_ARRAY_SIZE = 12
private const val AUTH_TAG_LENGTH = 128 // bit

/**
 * The implementation of these methods are heavily based on this:
 * https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf
 * and for a deeper knowledge please read this article:
 * https://levelup.gitconnected.com/doing-aes-gcm-in-android-adventures-in-the-field-72617401269d
 *
 */

fun encryptWithAes(
    input: ByteArray,
    encryptionKey: ByteArray
): ByteArray {
    val secretKey: SecretKey = SecretKeySpec(encryptionKey, AES_ALGORITHM)

    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val ivBytes = ByteArray(IV_BYTE_ARRAY_SIZE)
    val parameterSpec = GCMParameterSpec(AUTH_TAG_LENGTH, ivBytes)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

    val ciphertext: ByteArray = cipher.doFinal(input)
    val iv: ByteArray = cipher.iv

    val byteBuffer = ByteBuffer.allocate(iv.size + ciphertext.size)
    byteBuffer.put(iv)
    byteBuffer.put(ciphertext)

    return byteBuffer.array()
}

fun decryptWithAes(
    input: ByteArray,
    encryptionKey: ByteArray
): String {
    val secretKey: SecretKey = SecretKeySpec(encryptionKey, AES_ALGORITHM)

    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val gcmIv: AlgorithmParameterSpec =
        GCMParameterSpec(AUTH_TAG_LENGTH, input, 0, IV_BYTE_ARRAY_SIZE)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)

    val plainText = cipher.doFinal(input, IV_BYTE_ARRAY_SIZE, input.size - IV_BYTE_ARRAY_SIZE)

    return String(plainText, StandardCharsets.UTF_8)
}
