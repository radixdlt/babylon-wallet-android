package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.decodeHex
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.serialisers.InstantSerializer
import rdx.works.profile.data.utils.hashToFactorId
import java.time.Instant

@Serializable(with = FactorSourceSerializer::class)
sealed class FactorSource {

    @SerialName("id")
    abstract val id: FactorSourceID

    @SerialName("common")
    abstract val common: Common

    @Serializable(with = FactorSourceIDSerializer::class)
    sealed class FactorSourceID {

        @SerialName("kind")
        abstract val kind: FactorSourceKind

        @Serializable
        @SerialName(fromHashSerialName)
        data class FromHash(
            override val kind: FactorSourceKind,
            @SerialName("body")
            val body: HexCoded32Bytes
        ) : FactorSourceID()

        @Serializable
        @SerialName(fromAddressSerialName)
        data class FromAddress(
            override val kind: FactorSourceKind,
            @SerialName("body")
            val body: AccountAddress
        ) : FactorSourceID()

        companion object {
            const val fromHashSerialName = "fromHash"
            const val fromAddressSerialName = "fromAddress"
        }
    }

    // TODO move it to the domain layer
    @Serializable
    @JvmInline
    value class HexCoded32Bytes(val value: String) {
        init {
            val byteArray = value.decodeHex().toByteArray()
            require(byteArray.size == byteCount) { "value must be 32 bytes but it is ${byteArray.size}" }
        }

        companion object {
            private const val byteCount = 32
        }
    }

    // TODO move it to the domain layer
    @Serializable
    @JvmInline
    value class AccountAddress(val value: String)

    @Serializable
    data class Hint(
        @SerialName("model")
        val model: String,
        @SerialName("name")
        val name: String
    )

    @Serializable
    data class Common(
        @SerialName("cryptoParameters")
        val cryptoParameters: CryptoParameters,

        /**
         * When this factor source for originally added by the user.
         */
        @Serializable(with = InstantSerializer::class)
        @SerialName("addedOn")
        val addedOn: Instant,

        /**
         * Date of last usage of this factor source
         */
        @Serializable(with = InstantSerializer::class)
        @SerialName("lastUsedOn")
        var lastUsedOn: Instant,

        @SerialName("flags")
        var flags: List<FactorSourceFlag> = emptyList()
    ) {

        @Serializable
        data class CryptoParameters(
            @SerialName("supportedCurves")
            val supportedCurves: LinkedHashSet<Slip10Curve>,
            /**
             * Can be empty if the factor source does not support HD derivation
             */
            @SerialName("supportedDerivationPathSchemes")
            val supportedDerivationPathSchemes: LinkedHashSet<DerivationPathScheme>
        ) {

            val supportsOlympia: Boolean
                get() = supportedCurves.contains(Slip10Curve.SECP_256K1) &&
                        supportedDerivationPathSchemes.contains(DerivationPathScheme.BIP_44_OLYMPIA)

            companion object {
                val babylon = CryptoParameters(
                    supportedCurves = linkedSetOf(Slip10Curve.CURVE_25519),
                    supportedDerivationPathSchemes = linkedSetOf(DerivationPathScheme.CAP_26)
                )

                val trustedEntity = CryptoParameters(
                    supportedCurves = linkedSetOf(Slip10Curve.CURVE_25519),
                    supportedDerivationPathSchemes = linkedSetOf()
                )

                val olympiaBackwardsCompatible = CryptoParameters(
                    supportedCurves = linkedSetOf(Slip10Curve.SECP_256K1, Slip10Curve.CURVE_25519),
                    supportedDerivationPathSchemes = linkedSetOf(
                        DerivationPathScheme.CAP_26,
                        DerivationPathScheme.BIP_44_OLYMPIA
                    )
                )

                val default = babylon
            }
        }
    }

    companion object {
        fun factorSourceId(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            curve: Slip10Curve = Slip10Curve.CURVE_25519,
            derivationPath: DerivationPath = DerivationPath.forFactorSource(),
        ): String {
            return mnemonicWithPassphrase.compressedPublicKey(
                curve = curve,
                derivationPath = derivationPath
            ).hashToFactorId()
        }
    }
}

class WasNotDeviceFactorSource : RuntimeException()
