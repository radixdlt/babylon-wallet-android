@file:OptIn(ExperimentalSerializationApi::class)

package rdx.works.core.encryption

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import rdx.works.core.decryptData
import rdx.works.core.encryptData
import javax.crypto.SecretKey

@Serializable(with = EncryptionSchemeSerializer::class)
sealed interface EncryptionScheme {

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray
    fun decrypt(data: ByteArray, key: SecretKey): ByteArray

    @Serializable
    class Version1 : EncryptionScheme {
        @EncodeDefault
        val version: Int = 1
        @EncodeDefault
        val description: String = "AESGCM-256"

        override fun encrypt(data: ByteArray, key: SecretKey): ByteArray = encryptData(data, key).getOrThrow()

        override fun decrypt(data: ByteArray, key: SecretKey): ByteArray = decryptData(data, key).getOrThrow()
    }

    companion object {
        val default: EncryptionScheme = Version1()
    }
}

internal class EncryptionSchemeSerializer: JsonContentPolymorphicSerializer<EncryptionScheme>(
    EncryptionScheme::class
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<EncryptionScheme> {
        return when (val version = element.jsonObject["version"]?.jsonPrimitive?.intOrNull) {
            1 -> EncryptionScheme.Version1.serializer()
            else -> error("Not supported EncryptionScheme version $version")
        }
    }
}
