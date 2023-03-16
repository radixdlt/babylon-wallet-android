package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Slip10Curve {
    @SerialName("curve25519")
    CURVE_25519,

    @SerialName("secp256k1")
    SECP_256K1
}
