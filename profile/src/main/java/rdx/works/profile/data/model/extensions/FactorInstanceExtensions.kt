package rdx.works.profile.data.model.extensions

import rdx.works.core.decodeHex
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance

@Suppress("MagicNumber")
val FactorInstance.PublicKey.isInvalidCurve25519Key: Boolean
    get() {
        return curve == Slip10Curve.CURVE_25519 && compressedData.decodeHex().size != 32
    }
