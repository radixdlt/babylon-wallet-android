package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.decodeHex
import rdx.works.core.encryption.EncryptionScheme
import rdx.works.core.encryption.KeyDerivationScheme
import rdx.works.core.toHexString

@Serializable
data class EncryptedProfileSnapshot(
    @SerialName("encryptedSnapshot")
    val encryptedSnapshot: String,
    @SerialName("encryptionScheme")
    val encryptionScheme: EncryptionScheme,
    @SerialName("keyDerivationScheme")
    val keyDerivationScheme: KeyDerivationScheme
) {

    internal fun decrypt(password: String): String {
        val key = keyDerivationScheme.derive(password)
        return encryptionScheme.decrypt(encryptedSnapshot.decodeHex(), key).decodeToString()
    }

    companion object {
        internal fun from(snapshot: String, password: String): EncryptedProfileSnapshot {
            val keyDerivationScheme = KeyDerivationScheme.default
            val encryptionScheme = EncryptionScheme.default

            return EncryptedProfileSnapshot(
                encryptedSnapshot = encryptionScheme.encrypt(
                    data = snapshot.toByteArray(),
                    key = keyDerivationScheme.derive(password)
                ).toHexString(),
                encryptionScheme = encryptionScheme,
                keyDerivationScheme = keyDerivationScheme
            )
        }
    }

}
