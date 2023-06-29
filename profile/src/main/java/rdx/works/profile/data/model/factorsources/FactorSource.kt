package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rdx.works.core.InstantGenerator
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
     * A user facing **label** about this FactorSource which is displayed
     * to the user when she is prompted for this FactorSource during
     * for example transaction signing. For most FactorSource kinds
     * this value will be a *name*, here are some examples:
     *
     * * `.device`: "iPhone RED"
     * * `.ledgerHQHardwareWallet`: "Ledger MOON Edition"
     * * `.trustedEntity`: "Sarah"
     * * `.offDeviceMnemonic`: "Story about a horse and a battery"
     * * `.securityQuestion`: ""
     *
     * The reason why this is mutable (`var`) instead of immutable `let` is
     * an implementation detailed on iOS, where reading the device name
     * and model is `async` but we want to be able to `sync` create the
     * profile, thus this property at a later point in time where an async
     * context is available.
     */
    @SerialName("label")
    val label: String,

    /** A user facing **description** about this FactorSource which is displayed
     * to the user when she is prompted for this FactorSource during
     * for example transaction signing. For most FactorSource kinds
     * this value will be a *model*, here are some examples:
     *
     * * `.device`: "iPhone SE 2nd gen"
     * * `.ledgerHQHardwareWallet`: "nanoS+"
     * * `.trustedEntity`: "Friend"
     * * `.offDeviceMnemonic`: "Stored in the place where I played often with my friend A***"
     * * `.securityQuestion`: ""
     *
     * The reason why this is mutable (`var`) instead of immutable `let` is
     * an implementation detailed on iOS, where reading the device name
     * and model is `async` but we want to be able to `sync` create the
     * profile, thus this property at a later point in time where an async
     * context is available.
     */
    @SerialName("description")
    val description: String,

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
    val storage: Storage? = null
) {

    fun getNextAccountDerivationIndex(forNetworkId: NetworkId): Int {
        val entityCreatingStorage = storage as? Storage.EntityCreating ?: throw WasNotDeviceFactorSource()

        return entityCreatingStorage.nextDerivationIndicesPerNetwork.find {
            it.networkId == forNetworkId.value
        }?.forAccount ?: 0
    }

    fun getNextIdentityDerivationIndex(forNetworkId: NetworkId): Int {
        val entityCreatingStorage = storage as? Storage.EntityCreating ?: throw WasNotDeviceFactorSource()

        return entityCreatingStorage.nextDerivationIndicesPerNetwork.find {
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
                supportedCurves = linkedSetOf(SECP_256K1, CURVE_25519),
                supportedDerivationPathSchemes = linkedSetOf(CAP_26, BIP_44_OLYMPIA)
            )
        }
    }

    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator(discriminator = "discriminator")
    sealed class Storage {

        @Serializable
        @SerialName("entityCreating")
        data class EntityCreating(
            @SerialName("nextDerivationIndicesPerNetwork")
            val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>
        ) : Storage() {

            fun incrementAccount(forNetworkId: NetworkId): EntityCreating {
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

            fun incrementIdentity(forNetworkId: NetworkId): EntityCreating {
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

    sealed class LedgerHardwareWallet {
        enum class DeviceModel {
            NanoS, NanoSPlus, NanoX;

            fun description(): String {
                return when (this) {
                    NanoS -> "nanoS"
                    NanoSPlus -> "nanoS+"
                    NanoX -> "nanoX"
                }
            }

            companion object {
                fun fromDescription(value: String): DeviceModel? {
                    return when (value) {
                        "nanoS" -> NanoS
                        "nanoS+" -> NanoSPlus
                        "nanoX" -> NanoX
                        else -> null
                    }
                }
            }
        }
    }

    companion object {
        fun babylon(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            label: String = "Android",
            description: String = "babylon",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            label = label,
            description = description,
            olympiaCompatible = false
        )

        fun olympia(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            label: String = "Android",
            description: String = "olympia",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            label = label,
            description = description,
            olympiaCompatible = true
        )

        fun ledger(
            id: ID,
            model: LedgerHardwareWallet.DeviceModel,
            name: String?,
            olympiaCompatible: Boolean = true
        ): FactorSource {
            return FactorSource(
                kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                id = id,
                label = name?.ifEmpty { "Unnamed ${model.description()}" } ?: "Unnamed ${model.description()}",
                description = model.description(),
                parameters = if (olympiaCompatible) olympia else babylon,
                storage = Storage.EntityCreating(nextDerivationIndicesPerNetwork = listOf()),
                addedOn = InstantGenerator(),
                lastUsedOn = InstantGenerator()
            )
        }

        private fun device(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            label: String,
            description: String,
            olympiaCompatible: Boolean
        ) = FactorSource(
            kind = FactorSourceKind.DEVICE,
            id = factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase),
            label = label,
            description = description,
            parameters = if (olympiaCompatible) olympia else babylon,
            storage = Storage.EntityCreating(nextDerivationIndicesPerNetwork = listOf()),
            addedOn = InstantGenerator(),
            lastUsedOn = InstantGenerator()
        )

        fun factorSourceId(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            curve: Slip10Curve = CURVE_25519,
            derivationPath: DerivationPath = DerivationPath.forFactorSource(),
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
