@file:OptIn(ExperimentalSerializationApi::class)

package rdx.works.core.encryption

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import javax.crypto.SecretKey

@Serializable
@JsonClassDiscriminator("version")
sealed interface EncryptionScheme {

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray
    fun decrypt(data: ByteArray, key: SecretKey): ByteArray

    @Serializable
    @SerialName("1")
    class Version1: EncryptionScheme {
        @EncodeDefault
        val description: String = "AESGCM-256"

        override fun encrypt(data: ByteArray, key: SecretKey): ByteArray = data.encrypt(secretKey = key)

        override fun decrypt(data: ByteArray, key: SecretKey): ByteArray = data.decrypt(secretKey = key)
    }

    companion object {
        val default: EncryptionScheme = Version1()
    }
}
