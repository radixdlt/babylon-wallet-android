@file:Suppress("TooManyFunctions")

package rdx.works.core

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import rdx.works.core.KeystoreManager.Companion.KEY_ALIAS_MNEMONIC
import rdx.works.core.KeystoreManager.Companion.KEY_ALIAS_PROFILE
import rdx.works.core.KeystoreManager.Companion.PROVIDER
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

/**
 * The implementation of these methods are heavily based on this:
 * https://gist.github.com/patrickfav/7e28d4eb4bf500f7ee8012c4a0cf7bbf
 * and for a deeper knowledge please read this article:
 * https://levelup.gitconnected.com/doing-aes-gcm-in-android-adventures-in-the-field-72617401269d
 */

private fun getOrCreateSecretKey(keySpec: KeySpec): SecretKey {
    return getSecretKey(keySpec.alias) ?: generateAesKey(keySpec)
}

@Suppress("SwallowedException")
private fun generateAesKey(keySpec: KeySpec): SecretKey {
    when (keySpec) {
        is KeySpec.Cache,
        is KeySpec.Profile -> {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER)
            val builder = KeyGenParameterSpec.Builder(keySpec.alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                // This is required to be able to provide the IV ourselves
                .setRandomizedEncryptionRequired(false)
                .setKeySize(AES_KEY_SIZE)
                .build()
            keyGenerator.init(builder)
            return keyGenerator.generateKey()
        }

        is KeySpec.Mnemonic -> {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER)
            val keygenParameterSpecBuilder = KeyGenParameterSpec.Builder(keySpec.alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                // This is required to be able to provide the IV ourselves
                .setRandomizedEncryptionRequired(false)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .setKeySize(AES_KEY_SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                keygenParameterSpecBuilder.setUserAuthenticationParameters(
                    KEY_AUTHORIZATION_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                keygenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(KEY_AUTHORIZATION_SECONDS)
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                keygenParameterSpecBuilder.setIsStrongBoxBacked(true)
                try {
                    keyGenerator.init(keygenParameterSpecBuilder.build())
                    keyGenerator.generateKey()
                } catch (e: StrongBoxUnavailableException) {
                    keygenParameterSpecBuilder.setIsStrongBoxBacked(false)
                    keyGenerator.init(keygenParameterSpecBuilder.build())
                    keyGenerator.generateKey()
                }
            } else {
                keyGenerator.init(keygenParameterSpecBuilder.build())
                keyGenerator.generateKey()
            }
        }
    }
}

private fun getSecretKey(keyAlias: String): SecretKey? {
    val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    return (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
}

fun decryptData(
    input: ByteArray,
    secretKey: SecretKey
): Result<ByteArray> {
    return runCatching {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val gcmIv: AlgorithmParameterSpec =
            GCMParameterSpec(AUTH_TAG_LENGTH, input, 0, GCM_IV_LENGTH)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)
        cipher.doFinal(input, GCM_IV_LENGTH, input.size - GCM_IV_LENGTH)
    }
}

fun encryptData(
    input: ByteArray,
    secretKey: SecretKey
): Result<ByteArray> {
    return runCatching {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val ivBytes = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(ivBytes)
        val parameterSpec = GCMParameterSpec(AUTH_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertext: ByteArray = cipher.doFinal(input)

        val byteBuffer = ByteBuffer.allocate(ivBytes.size + ciphertext.size)
        byteBuffer.put(ivBytes)
        byteBuffer.put(ciphertext)

        byteBuffer.array()
    }
}

fun String.encrypt(
    withKey: KeySpec
) = toByteArray().encrypt(withKey = withKey).map { Base64.encodeToString(it, Base64.DEFAULT) }

fun ByteArray.encrypt(
    withKey: KeySpec
): Result<ByteArray> = encryptData(input = this, secretKey = getOrCreateSecretKey(withKey))

fun ByteArray.encrypt(
    withEncryptionKey: ByteArray
): Result<ByteArray> = encryptData(input = this, secretKey = SecretKeySpec(withEncryptionKey, AES_ALGORITHM))

fun String.decrypt(
    withKey: KeySpec
): Result<String> {
    return Base64.decode(this, Base64.DEFAULT).decrypt(withKey).map { String(it, StandardCharsets.UTF_8) }
}

fun checkIfKeyWasPermanentlyInvalidated(input: String, key: KeySpec): Boolean {
    // on pixel 6 pro when I remove lock screen entirely, key entry for an alias is null
    val secretKey = getSecretKey(key.alias) ?: run {
        Log.d("Mnemonic security", "no key")
        return true
    }
    val result = encryptData(input.toByteArray(), secretKey)
    // according to documentation this is exception that should be thrown if we try to use invalidated key, but behavior I saw
    // when removing lock screen is that key is automatically deleted from the keystore
    return result.exceptionOrNull() is KeyPermanentlyInvalidatedException
}

fun ByteArray.decrypt(
    withKey: KeySpec
): Result<ByteArray> = decryptData(input = this, secretKey = getOrCreateSecretKey(withKey))

fun ByteArray.decrypt(
    withEncryptionKey: ByteArray
): Result<ByteArray> = decryptData(input = this, secretKey = SecretKeySpec(withEncryptionKey, AES_ALGORITHM))

const val AES_ALGORITHM = "AES"
private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
private const val AES_KEY_SIZE = 256
private const val GCM_IV_LENGTH = 12
private const val KEY_AUTHORIZATION_SECONDS = 10
private const val AUTH_TAG_LENGTH = 128 // bit

sealed class KeySpec(val alias: String) {
    class Profile(alias: String = KEY_ALIAS_PROFILE) : KeySpec(alias)
    class Mnemonic(alias: String = KEY_ALIAS_MNEMONIC) : KeySpec(alias)
    class Cache(alias: String) : KeySpec(alias)
}

@Suppress("MagicNumber")
fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
