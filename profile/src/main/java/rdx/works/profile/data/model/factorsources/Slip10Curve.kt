package rdx.works.profile.data.model.factorsources

import com.radixdlt.crypto.ec.EllipticCurveType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Slip10Curve {
    @SerialName("curve25519")
    CURVE_25519,

    @SerialName("secp256k1")
    SECP_256K1
}

fun Slip10Curve.toEllipticCurveType() = when (this) {
    Slip10Curve.CURVE_25519 -> EllipticCurveType.Ed25519
    Slip10Curve.SECP_256K1 -> EllipticCurveType.Secp256k1
}
