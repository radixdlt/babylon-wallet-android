package rdx.works.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
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
private const val KEY_ALIAS = "EncryptedDataStoreAlias"

/**
 * The implementation of these methods are heavily based on this:
 * https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf
 * and for a deeper knowledge please read this article:
 * https://levelup.gitconnected.com/doing-aes-gcm-in-android-adventures-in-the-field-72617401269d
 */

fun encryptData(
    input: ByteArray,
    encryptionKey: ByteArray? = null
): ByteArray {
    val secretKey: SecretKey = if (encryptionKey != null) {
        SecretKeySpec(encryptionKey, AES_ALGORITHM)
    } else {
        getOrCreateSecretKey()
    }

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

fun decryptData(
    input: ByteArray,
    encryptionKey: ByteArray? = null
): String {
    val secretKey: SecretKey = if (encryptionKey != null) {
        SecretKeySpec(encryptionKey, AES_ALGORITHM)
    } else {
        getOrCreateSecretKey()
    }

    val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
    val gcmIv: AlgorithmParameterSpec =
        GCMParameterSpec(AUTH_TAG_LENGTH, input, 0, GCM_IV_LENGTH)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)

    val plainText = cipher.doFinal(input, GCM_IV_LENGTH, input.size - GCM_IV_LENGTH)

    return String(plainText, StandardCharsets.UTF_8)
}

private fun getOrCreateSecretKey(): SecretKey {
    return getSecretKey() ?: generateAesKey()
}

private fun generateAesKey(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER)
    val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
        .setBlockModes(BLOCK_MODE_GCM)
        .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
        // This is required to be able to provide the IV ourselves
        .setRandomizedEncryptionRequired(false)
        .setKeySize(AES_KEY_SIZE)
        .build()
    keyGenerator.init(builder)
    return keyGenerator.generateKey()
}

private fun getSecretKey(): SecretKey? {
    val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    return (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
}
