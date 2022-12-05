package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FactorInstance(
    @SerialName("derivationPath")
    val derivationPath: DerivationPath,

    @SerialName("factorInstanceID")
    val factorInstanceID: String,

    @SerialName("factorSourceReference")
    val factorSourceReference: FactorSourceReference,

    @SerialName("initializationDate")
    val initializationDate: String,

    @SerialName("publicKey")
    val publicKey: PublicKey
) {
    @Serializable
    data class PublicKey(
        @SerialName("compressedData")
        val compressedData: String,

        @SerialName("curve")
        val curve: String
    ) {
        companion object {
            private const val curve25519 = "curve25519"
            fun curve25519PublicKey(
                compressedPublicKey: String
            ): PublicKey {
                return PublicKey(
                    compressedData = compressedPublicKey,
                    curve = curve25519
                )
            }
        }
    }
}
