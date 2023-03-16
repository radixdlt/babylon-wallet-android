package rdx.works.profile.data.model.factorsources

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.crypto.ec.EllipticCurveType
import java.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.profile.data.extensions.compressedPublicKey
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.BIP_44_OLYMPIA
import rdx.works.profile.data.model.factorsources.DerivationPathScheme.CAP_26
import rdx.works.profile.data.model.factorsources.FactorSource.Parameters.Companion.babylon
import rdx.works.profile.data.model.factorsources.FactorSource.Parameters.Companion.olympiaBackwardsCompatible
import rdx.works.profile.data.model.factorsources.Slip10Curve.CURVE_25519
import rdx.works.profile.data.model.factorsources.Slip10Curve.SECP_256K1
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.serialisers.InstantSerializer
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.derivation.CustomHDDerivationPath
import rdx.works.profile.derivation.model.NetworkId

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
    val id: String,

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

            val olympiaBackwardsCompatible = Parameters(
                supportedCurves = linkedSetOf(CURVE_25519, SECP_256K1),
                supportedDerivationPathSchemes = linkedSetOf(CAP_26, BIP_44_OLYMPIA)
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
            val nextDerivationIndicesPerNetwork: List<OnNetwork.NextDerivationIndices>
        ): Storage() {

            fun incrementAccount(forNetworkId: NetworkId): Device {
                val indicesForNetwork = nextDerivationIndicesPerNetwork.find {
                    it.networkId == forNetworkId.value
                }

                val mutatedList = if (indicesForNetwork == null) {
                    nextDerivationIndicesPerNetwork + OnNetwork.NextDerivationIndices(
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
                    nextDerivationIndicesPerNetwork + OnNetwork.NextDerivationIndices(
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
            mnemonic: MnemonicWords,
            bip39Passphrase: String = "",
            hint: String = "babylon",
        ) = device(
            mnemonic = mnemonic,
            bip39Passphrase = bip39Passphrase,
            hint = hint,
            olympiaCompatible = false
        )

        fun olympia(
            mnemonic: MnemonicWords,
            bip39Passphrase: String = "",
            hint: String = "olympia",
        ) = device(
            mnemonic = mnemonic,
            bip39Passphrase = bip39Passphrase,
            hint = hint,
            olympiaCompatible = true
        )

        fun device(
            mnemonic: MnemonicWords,
            hint: String,
            bip39Passphrase: String = "",
            olympiaCompatible: Boolean
        ) = FactorSource(
            kind = FactorSourceKind.DEVICE,
            id = factorSourceId(
                mnemonic = mnemonic,
                bip39Passphrase = bip39Passphrase
            ),
            hint = hint,
            parameters = if (olympiaCompatible) olympiaBackwardsCompatible else babylon,
            storage = Storage.Device(nextDerivationIndicesPerNetwork = listOf()),
            addedOn = Instant.now(),
            lastUsedOn = Instant.now()
        )

        fun factorSourceId(
            mnemonic: MnemonicWords,
            ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
            derivationPath: String = CustomHDDerivationPath.getId.path,
            bip39Passphrase: String = ""
        ): String {
            return mnemonic.compressedPublicKey(
                ellipticCurveType = ellipticCurveType,
                derivationPath = derivationPath,
                bip39Passphrase = bip39Passphrase
            ).hashToFactorId()
        }
    }
}

class WasNotDeviceFactorSource: RuntimeException()
