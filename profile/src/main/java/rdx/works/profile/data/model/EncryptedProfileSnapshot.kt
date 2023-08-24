package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.decodeHex
import rdx.works.core.encryption.EncryptionScheme
import rdx.works.core.encryption.KeyDerivationScheme
import rdx.works.core.toHexString

@Serializable
data class EncryptedProfileSnapshot(
    @SerialName("version")
    val version: Version,
    @SerialName("encryptedSnapshot")
    val encryptedSnapshot: String,
    @SerialName("encryptionScheme")
    val encryptionScheme: EncryptionScheme,
    @SerialName("keyDerivationScheme")
    val keyDerivationScheme: KeyDerivationScheme
) {

    @JvmInline
    @Serializable
    value class Version(val version: Int) {

        companion object {
            val CURRENT = Version(version = 1)
        }

    }

    internal fun decrypt(deserializer: Json, password: String): ProfileSnapshot {
        val key = keyDerivationScheme.derive(password)
        val decrypted = encryptionScheme.decrypt(encryptedSnapshot.decodeHex(), key).decodeToString()
        return deserializer.decodeFromString(decrypted)
    }

    companion object {
        internal fun from(serializer: Json, snapshot: ProfileSnapshot, password: String): EncryptedProfileSnapshot {
            val snapshotString = serializer.encodeToString(snapshot)
            return from(snapshotString, password)
        }

        internal fun from(snapshotString: String, password: String): EncryptedProfileSnapshot {
            val keyDerivationScheme = KeyDerivationScheme.default
            val encryptionScheme = EncryptionScheme.default

            return EncryptedProfileSnapshot(
                version = Version.CURRENT,
                encryptedSnapshot = encryptionScheme.encrypt(
                    data = snapshotString.toByteArray(),
                    key = keyDerivationScheme.derive(password)
                ).toHexString(),
                encryptionScheme = encryptionScheme,
                keyDerivationScheme = keyDerivationScheme
            )
        }
    }

}
