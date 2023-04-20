package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.BIP_44_OLYMPIA
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.CAP_26
import rdx.works.profile.data.model.factorsources.FactorSource.Parameters.Companion.babylon
import rdx.works.profile.data.model.factorsources.FactorSource.Parameters.Companion.olympia
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.factorsources.Slip10Curve.SECP_256K1
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.serialisers.InstantSerializer
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.derivation.model.NetworkId
import java.time.Instant

/**
 * A FactorSource is the source of FactorInstance(s)
 */
@Serializable
data class FactorSource(
    /**
     * The kind of the factor source.
     */
    @SerialName("kind")
    val kind: FactorSourceKind,

    /**
     * Canonical identifier which uniquely identifies this factor source.
     */
    @SerialName("id")
    val id: ID,

    /**
     * A user facing hint about this FactorSource which is displayed
     * to the user when she is prompted for this FactorSource during
     * for example transaction signing. Here are some examples.
     *
     * * "iPhone 14 Pro Max",
     * * "Google Pixel 6",
     * * "Ledger Nano Model X",
     * * "My friend Lisa"
     * * "YubiKey 5C NFC"
     * * "Just a private key put in my standard secure storage."
     * * "Mnemonic that describes a saga about a crazy horse"
     */
    @SerialName("hint")
    val hint: String,

    /**
     * Curve/Derivation scheme
     */
    @SerialName("parameters")
    val parameters: Parameters,

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
    val lastUsedOn: Instant,

    /**
     * Some factor source requires extra stored properties, e.g.
     * [FactorSourceKind.SECURITY_QUESTIONS] kind which requires storage of:
     * * which questions user chose
     * * the encryptions of the mnemonic
     *
     * Rather than letting ALL factor source contain ALL possible
     * extra stored properties as optionals, which will be `null`
     * for most FactorSource, we model this with one single optional
     * being a sealed class modelling all possible required extra stored
     * properties.
     */
    @SerialName("storage")
    val storage: Storage?
) {

    fun getNextAccountDerivationIndex(forNetworkId: NetworkId): Int {
        val deviceStorage = storage as? Storage.Device ?: throw WasNotDeviceFactorSource()

        return deviceStorage.nextDerivationIndicesPerNetwork.find {
            it.networkId == forNetworkId.value
        }?.forAccount ?: 0
    }

    fun getNextIdentityDerivationIndex(forNetworkId: NetworkId): Int {
        val deviceStorage = storage as? Storage.Device ?: throw WasNotDeviceFactorSource()

        return deviceStorage.nextDerivationIndicesPerNetwork.find {
            it.networkId == forNetworkId.value
        }?.forIdentity ?: 0
    }

    fun supportsCurve(curve: Slip10Curve) = parameters.supportedCurves.contains(curve)

    @JvmInline
    @Serializable
    value class ID(
        val value: String
    )

    @Serializable
    data class Parameters(
        @SerialName("supportedCurves")
        val supportedCurves: LinkedHashSet<Slip10Curve>,
        /**
         * Can be empty if the factor source does not support HD derivation
         */
        @SerialName("supportedDerivationPathSchemes")
        val supportedDerivationPathSchemes: LinkedHashSet<DerivationPathScheme>
    ) {

        val supportsOlympia: Boolean
            get() = supportedCurves.contains(SECP_256K1) &&
                supportedDerivationPathSchemes.contains(BIP_44_OLYMPIA)

        companion object {
            val babylon = Parameters(
                supportedCurves = linkedSetOf(CURVE_25519),
                supportedDerivationPathSchemes = linkedSetOf(CAP_26)
            )

            val olympia = Parameters(
                supportedCurves = linkedSetOf(SECP_256K1),
                supportedDerivationPathSchemes = linkedSetOf(BIP_44_OLYMPIA)
            )
        }
    }

    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator(discriminator = "discriminator")
    sealed class Storage {

        @Serializable
        @SerialName("device")
        data class Device(
            @SerialName("nextDerivationIndicesPerNetwork")
            val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>
        ) : Storage() {

            fun incrementAccount(forNetworkId: NetworkId): Device {
                val indicesForNetwork = nextDerivationIndicesPerNetwork.find {
                    it.networkId == forNetworkId.value
                }

                val mutatedList = if (indicesForNetwork == null) {
                    nextDerivationIndicesPerNetwork + Network.NextDerivationIndices(
                        networkId = forNetworkId.value,
                        forAccount = 1,
                        forIdentity = 0
                    )
                } else {
                    nextDerivationIndicesPerNetwork.map {
                        if (it.networkId == forNetworkId.value) {
                            it.copy(forAccount = it.forAccount + 1)
                        } else {
                            it
                        }
                    }
                }

                return copy(nextDerivationIndicesPerNetwork = mutatedList)
            }

            fun incrementIdentity(forNetworkId: NetworkId): Device {
                val indicesForNetwork = nextDerivationIndicesPerNetwork.find {
                    it.networkId == forNetworkId.value
                }

                val mutatedList = if (indicesForNetwork == null) {
                    nextDerivationIndicesPerNetwork + Network.NextDerivationIndices(
                        networkId = forNetworkId.value,
                        forAccount = 0,
                        forIdentity = 1
                    )
                } else {
                    nextDerivationIndicesPerNetwork.map {
                        if (it.networkId == forNetworkId.value) {
                            it.copy(forIdentity = it.forIdentity + 1)
                        } else {
                            it
                        }
                    }
                }

                return copy(nextDerivationIndicesPerNetwork = mutatedList)
            }
        }
    }

    companion object {
        fun babylon(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            hint: String = "babylon",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            hint = hint,
            olympiaCompatible = false
        )

        fun olympia(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            hint: String = "olympia",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            hint = hint,
            olympiaCompatible = true
        )

        private fun device(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            hint: String,
            olympiaCompatible: Boolean
        ) = FactorSource(
            kind = FactorSourceKind.DEVICE,
            id = factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase),
            hint = hint,
            parameters = if (olympiaCompatible) olympia else babylon,
            storage = Storage.Device(nextDerivationIndicesPerNetwork = listOf()),
            addedOn = Instant.now(),
            lastUsedOn = Instant.now()
        )

        fun factorSourceId(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            curve: Slip10Curve = CURVE_25519,
            derivationPath: String = DerivationPath.forFactorSource().path,
        ): ID {
            return ID(
                mnemonicWithPassphrase.compressedPublicKey(
                    curve = curve,
                    derivationPath = derivationPath
                ).hashToFactorId()
            )
        }
    }
}

class WasNotDeviceFactorSource : RuntimeException()
