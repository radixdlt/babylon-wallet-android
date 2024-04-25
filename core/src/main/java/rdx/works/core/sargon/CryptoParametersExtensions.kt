package rdx.works.core.sargon

import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.FactorSourceCryptoParameters
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.SupportedCurves
import com.radixdlt.sargon.extensions.contains
import com.radixdlt.sargon.extensions.init

val FactorSourceCryptoParameters.Companion.babylon: FactorSourceCryptoParameters
    get() = FactorSourceCryptoParameters(
        supportedCurves = SupportedCurves.init(Slip10Curve.CURVE25519),
        supportedDerivationPathSchemes = listOf(DerivationPathScheme.CAP26)
    )

val FactorSourceCryptoParameters.Companion.olympia: FactorSourceCryptoParameters
    get() = FactorSourceCryptoParameters(
        supportedCurves = SupportedCurves.init(Slip10Curve.SECP256K1),
        supportedDerivationPathSchemes = listOf(DerivationPathScheme.BIP44_OLYMPIA)
    )

val FactorSourceCryptoParameters.Companion.olympiaBackwardsCompatible: FactorSourceCryptoParameters
    get() = FactorSourceCryptoParameters(
        supportedCurves = SupportedCurves.init(Slip10Curve.SECP256K1, Slip10Curve.CURVE25519),
        supportedDerivationPathSchemes = listOf(DerivationPathScheme.CAP26, DerivationPathScheme.BIP44_OLYMPIA)
    )

val FactorSourceCryptoParameters.Companion.trustedEntity: FactorSourceCryptoParameters
    get() = FactorSourceCryptoParameters(
        supportedCurves = SupportedCurves.init(Slip10Curve.CURVE25519),
        supportedDerivationPathSchemes = emptyList()
    )

val FactorSourceCryptoParameters.Companion.default: FactorSourceCryptoParameters
    get() = babylon

val FactorSourceCryptoParameters.supportsBabylon: Boolean
    get() = Slip10Curve.CURVE25519 in supportedCurves &&
        DerivationPathScheme.CAP26 in supportedDerivationPathSchemes

val FactorSourceCryptoParameters.supportsOlympia: Boolean
    get() = Slip10Curve.SECP256K1 in supportedCurves &&
        DerivationPathScheme.BIP44_OLYMPIA in supportedDerivationPathSchemes
