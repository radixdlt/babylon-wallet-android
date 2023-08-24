package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.factorsources.Slip10Curve.SECP_256K1

@Serializable
data class FactorInstance(
    @SerialName("badge")
    val badge: Badge,

    @SerialName("factorSourceID")
    val factorSourceId: FactorSource.FactorSourceID
) {

    @Serializable(with = BadgeSerializer::class)
    sealed class Badge {

        @Serializable(with = VirtualSourceSerializer::class)
        sealed class VirtualSource : Badge() {

            @Serializable
            @SerialName(hierarchicalDeterministicPublicKey)
            data class HierarchicalDeterministic(
                @SerialName("derivationPath")
                val derivationPath: DerivationPath,

                @SerialName("publicKey")
                val publicKey: PublicKey
            ) : VirtualSource()

            companion object {
                const val hierarchicalDeterministicPublicKey = "hierarchicalDeterministicPublicKey"
            }
        }

        companion object {
            const val virtualSource = "virtualSource"
        }
    }

    @Serializable
    data class PublicKey(
        @SerialName("compressedData")
        val compressedData: String,

        @SerialName("curve")
        val curve: Slip10Curve
    ) {
        companion object {
            fun curve25519PublicKey(
                compressedPublicKey: String
            ): PublicKey {
                return PublicKey(
                    compressedData = compressedPublicKey,
                    curve = CURVE_25519
                )
            }

            fun curveSecp256k1PublicKey(
                compressedPublicKey: String
            ): PublicKey {
                return PublicKey(
                    compressedData = compressedPublicKey,
                    curve = SECP_256K1
                )
            }
        }
    }
}
