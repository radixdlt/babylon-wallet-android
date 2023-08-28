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
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Serializable(with = KeyDerivationSchemeSerializer::class)
sealed interface KeyDerivationScheme {

    fun derive(password: String): SecretKey

    @Serializable
    class Version1 : KeyDerivationScheme {
        @EncodeDefault
        val version: Int = 1

        @EncodeDefault
        val description: String = "HKDFSHA256-with-UTF8-encoding-of-password-no-salt-no-info"

        @Suppress("MagicNumber")
        override fun derive(password: String): SecretKey {
            val generator = HKDFBytesGenerator(SHA256Digest()).apply {
                init(HKDFParameters.defaultParameters(password.toByteArray(Charsets.UTF_8)))
            }

            val byteArray = ByteArray(32)
            generator.generateBytes(byteArray, 0, 32)
            return SecretKeySpec(
                byteArray,
                "AES"
            )
        }
    }

    companion object {
        val default: KeyDerivationScheme = Version1()
    }
}

internal class KeyDerivationSchemeSerializer : JsonContentPolymorphicSerializer<KeyDerivationScheme>(
    KeyDerivationScheme::class
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<KeyDerivationScheme> {
        return when (val version = element.jsonObject["version"]?.jsonPrimitive?.intOrNull) {
            1 -> KeyDerivationScheme.Version1.serializer()
            else -> error("Not supported KeyDerivationScheme version $version")
        }
    }
}
