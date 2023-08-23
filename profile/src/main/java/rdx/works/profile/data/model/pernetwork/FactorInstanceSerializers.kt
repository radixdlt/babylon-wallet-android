package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import rdx.works.profile.data.model.factorsources.FactorSourceSurrogate

@Serializable
data class BadgeSurrogate(
    val discriminator: String,
    val virtualSource: FactorInstance.Badge.VirtualSource? = null
)

object BadgeSerializer : KSerializer<FactorInstance.Badge> {
    override val descriptor: SerialDescriptor = FactorSourceSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: FactorInstance.Badge) {
        val surrogate = when (value) {
            is FactorInstance.Badge.VirtualSource -> {
                BadgeSurrogate(
                    discriminator = FactorInstance.Badge.virtualSource,
                    virtualSource = value
                )
            }
        }
        encoder.encodeSerializableValue(BadgeSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): FactorInstance.Badge {
        val surrogate = decoder.decodeSerializableValue(BadgeSurrogate.serializer())
        return when (surrogate.discriminator) {
            FactorInstance.Badge.virtualSource -> {
                surrogate.virtualSource!!
            }

            else -> {
                error("not supported Badge discriminator")
            }
        }
    }
}

@Serializable
data class VirtualSourceSurrogate(
    val discriminator: String,
    val hierarchicalDeterministicPublicKey: FactorInstance.Badge.VirtualSource.HierarchicalDeterministic? = null
)

object VirtualSourceSerializer : KSerializer<FactorInstance.Badge.VirtualSource> {
    override val descriptor: SerialDescriptor = FactorSourceSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: FactorInstance.Badge.VirtualSource) {
        val surrogate = when (value) {
            is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                VirtualSourceSurrogate(
                    discriminator = FactorInstance.Badge.VirtualSource.hierarchicalDeterministicPublicKey,
                    hierarchicalDeterministicPublicKey = value
                )
            }
        }
        encoder.encodeSerializableValue(VirtualSourceSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): FactorInstance.Badge.VirtualSource {
        val surrogate = decoder.decodeSerializableValue(VirtualSourceSurrogate.serializer())
        return when (surrogate.discriminator) {
            FactorInstance.Badge.VirtualSource.hierarchicalDeterministicPublicKey -> {
                surrogate.hierarchicalDeterministicPublicKey!!
            }

            else -> {
                error("not supported VirtualSource discriminator")
            }
        }
    }
}
