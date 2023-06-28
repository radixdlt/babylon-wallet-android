package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class FactorSourceSurrogate(
    val discriminator: String,
    val device: DeviceFactorSource? = null,
    val ledgerHQHardwareWallet: LedgerHardwareWalletFactorSource? = null,
    val offDeviceMnemonic: OffDeviceMnemonicFactorSource? = null,
    val trustedContact: TrustedContactFactorSource? = null
)

object FactorSourceSerializer : KSerializer<FactorSource> {
    override val descriptor: SerialDescriptor = FactorSourceSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: FactorSource) {
        val surrogate = when (value) {
            is DeviceFactorSource -> {
                FactorSourceSurrogate(
                    discriminator = FactorSourceKind.deviceSerialName,
                    device = value
                )
            }
            is LedgerHardwareWalletFactorSource -> {
                FactorSourceSurrogate(
                    discriminator = FactorSourceKind.ledgerHQHardwareWalletSerialName,
                    ledgerHQHardwareWallet = value
                )
            }
            is OffDeviceMnemonicFactorSource -> {
                FactorSourceSurrogate(
                    discriminator = FactorSourceKind.offDeviceMnemonicSerialName,
                    offDeviceMnemonic = value
                )
            }
            is TrustedContactFactorSource -> {
                FactorSourceSurrogate(
                    discriminator = FactorSourceKind.trustedContactSerialName,
                    trustedContact = value
                )
            }
        }
        encoder.encodeSerializableValue(FactorSourceSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): FactorSource {
        val surrogate = decoder.decodeSerializableValue(FactorSourceSurrogate.serializer())
        return when (surrogate.discriminator) {
            FactorSourceKind.deviceSerialName -> {
                surrogate.device!!
            }
            FactorSourceKind.ledgerHQHardwareWalletSerialName -> {
                surrogate.ledgerHQHardwareWallet!!
            }
            FactorSourceKind.offDeviceMnemonicSerialName -> {
                surrogate.offDeviceMnemonic!!
            }
            FactorSourceKind.trustedContactSerialName -> {
                surrogate.trustedContact!!
            }
            else -> {
                error("not supported FactorSource discriminator")
            }
        }
    }
}

@Serializable
data class FactorSourceIDSurrogate(
    val discriminator: String,
    val fromHash: FactorSource.FactorSourceID.FromHash? = null,
    val fromAddress: FactorSource.FactorSourceID.FromAddress? = null
)

object FactorSourceIDSerializer : KSerializer<FactorSource.FactorSourceID> {
    override val descriptor: SerialDescriptor = FactorSourceSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: FactorSource.FactorSourceID) {
        val surrogate = when (value) {
            is FactorSource.FactorSourceID.FromHash -> {
                FactorSourceIDSurrogate(
                    discriminator = FactorSource.FactorSourceID.fromHashSerialName,
                    fromHash = value
                )
            }
            is FactorSource.FactorSourceID.FromAddress -> {
                FactorSourceIDSurrogate(
                    discriminator = FactorSource.FactorSourceID.fromAddressSerialName,
                    fromAddress = value
                )
            }
        }
        encoder.encodeSerializableValue(FactorSourceIDSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): FactorSource.FactorSourceID {
        val surrogate = decoder.decodeSerializableValue(FactorSourceIDSurrogate.serializer())
        return when (surrogate.discriminator) {
            FactorSource.FactorSourceID.fromHashSerialName -> {
                surrogate.fromHash!!
            }
            FactorSource.FactorSourceID.fromAddressSerialName -> {
                surrogate.fromAddress!!
            }
            else -> {
                error("not supported FactorSourceID discriminator")
            }
        }
    }
}
