@file:OptIn(ExperimentalSerializationApi::class)

package rdx.works.core.encryption

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Serializable
@JsonClassDiscriminator("version")
sealed interface KeyDerivationScheme : VersionedAlgorithm {

    fun derive(password: String): SecretKey

    @Serializable
    @SerialName("1")
    class Version1 : KeyDerivationScheme {
        override val version: Int = 1
        override val description: String = "HKDFSHA256-with-UTF8-encoding-of-password-no-salt-no-info"

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
