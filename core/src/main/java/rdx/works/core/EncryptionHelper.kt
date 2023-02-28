package rdx.works.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.*
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_ALGORITHM = "AES"
private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
private const val AES_KEY_SIZE = 256
private const val GCM_IV_LENGTH = 12
private const val AUTH_TAG_LENGTH = 128 // bit
private const val PROVIDER = "AndroidKeyStore"

/**
 * The implementation of these methods are heavily based on this:
 * https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf
 * and for a deeper knowledge please read this article:
 * https://levelup.gitconnected.com/doing-aes-gcm-in-android-adventures-in-the-field-72617401269d
 */

fun String.encrypt(
    withKeyAlias: String
): String = Base64.encodeToString(
    this.toByteArray().encrypt(withKeyAlias = withKeyAlias),
    Base64.DEFAULT
)

fun ByteArray.encrypt(
    withKeyAlias: String
): ByteArray = encryptData(input = this, secretKey = getOrCreateSecretKey(withKeyAlias))

fun ByteArray.encrypt(
    withEncryptionKey: ByteArray
): ByteArray = encryptData(input = this, secretKey = SecretKeySpec(withEncryptionKey, AES_ALGORITHM))

private fun encryptData(
    input: ByteArray,
    secretKey: SecretKey
): ByteArray {
    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val ivBytes = ByteArray(GCM_IV_LENGTH)
    SecureRandom().nextBytes(ivBytes)
    val parameterSpec = GCMParameterSpec(AUTH_TAG_LENGTH, ivBytes)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

    val ciphertext: ByteArray = cipher.doFinal(input)

    val byteBuffer = ByteBuffer.allocate(ivBytes.size + ciphertext.size)
    byteBuffer.put(ivBytes)
    byteBuffer.put(ciphertext)

    return byteBuffer.array()
}

fun String.decrypt(
    withKeyAlias: String
): String {
    val decryptedBytes = Base64.decode(this, Base64.DEFAULT).decrypt(withKeyAlias)
    return String(decryptedBytes, StandardCharsets.UTF_8)
}

fun ByteArray.decrypt(
    withKeyAlias: String
): ByteArray = decryptData(input = this, secretKey = getOrCreateSecretKey(withKeyAlias))

fun ByteArray.decrypt(
    withEncryptionKey: ByteArray
): ByteArray = decryptData(input = this, secretKey = SecretKeySpec(withEncryptionKey, AES_ALGORITHM))

private fun decryptData(
    input: ByteArray,
    secretKey: SecretKey
): ByteArray {
    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val gcmIv: AlgorithmParameterSpec =
        GCMParameterSpec(AUTH_TAG_LENGTH, input, 0, GCM_IV_LENGTH)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)

    return cipher.doFinal(input, GCM_IV_LENGTH, input.size - GCM_IV_LENGTH)
}

private fun getOrCreateSecretKey(keyAlias: String): SecretKey {
    return getSecretKey(keyAlias) ?: generateAesKey(keyAlias)
}

private fun generateAesKey(keyAlias: String): SecretKey {
    val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER)
    val builder = KeyGenParameterSpec.Builder(keyAlias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
        .setBlockModes(BLOCK_MODE_GCM)
        .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
        // This is required to be able to provide the IV ourselves
        .setRandomizedEncryptionRequired(false)
        .setKeySize(AES_KEY_SIZE)
        .build()
    keyGenerator.init(builder)
    return keyGenerator.generateKey()
}

private fun getSecretKey(keyAlias: String): SecretKey? {
    val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    return (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
}
