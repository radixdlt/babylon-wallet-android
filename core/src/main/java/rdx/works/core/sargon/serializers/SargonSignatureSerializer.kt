package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class SargonSignatureSerializer : KSerializer<Signature> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.radixdlt.sargon.Signature", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Signature) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): Signature {
        return Signature.init(decoder.decodeString().hexToBagOfBytes())
    }
}

class SargonPublicKeySerializer : KSerializer<PublicKey> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.radixdlt.sargon.PublicKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PublicKey) {
        encoder.encodeString(value.hex)
    }

    override fun deserialize(decoder: Decoder): PublicKey {
        return PublicKey.init(decoder.decodeString())
    }
}

@Suppress("CyclomaticComplexMethod")
class SignatureWithPublicKeySerializer : KSerializer<SignatureWithPublicKey> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.radixdlt.sargon.SignatureWithPublicKey") {
            element<String>("type")
            element("publicKey", buildClassSerialDescriptor("publicKey"))
            element("signature", buildClassSerialDescriptor("signature"))
        }

    override fun serialize(encoder: Encoder, value: SignatureWithPublicKey) {
        encoder.encodeStructure(descriptor) {
            when (value) {
                is SignatureWithPublicKey.Secp256k1 -> {
                    encodeStringElement(descriptor, 0, Secp256k1)
                    encodeSerializableElement(descriptor, 1, SargonPublicKeySerializer(), value.publicKey.asGeneral())
                    encodeSerializableElement(descriptor, 2, SargonSignatureSerializer(), value.signature.asGeneral())
                }

                is SignatureWithPublicKey.Ed25519 -> {
                    encodeStringElement(descriptor, 0, Ed25519)
                    encodeSerializableElement(descriptor, 1, SargonPublicKeySerializer(), value.publicKey.asGeneral())
                    encodeSerializableElement(descriptor, 2, SargonSignatureSerializer(), value.signature.asGeneral())
                }
            }
        }
    }

    override fun deserialize(decoder: Decoder): SignatureWithPublicKey {
        return decoder.decodeStructure(descriptor) {
            var type: String? = null
            var publicKey: PublicKey? = null
            var signature: Signature? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> {
                        publicKey = when (type) {
                            Secp256k1 -> decodeSerializableElement(descriptor, 1, SargonPublicKeySerializer())
                            Ed25519 -> decodeSerializableElement(descriptor, 1, SargonPublicKeySerializer())
                            else -> throw SerializationException("Unknown type $type")
                        }
                    }

                    2 -> {
                        signature = when (type) {
                            Secp256k1 -> decodeSerializableElement(descriptor, 2, SargonSignatureSerializer())
                            Ed25519 -> decodeSerializableElement(descriptor, 2, SargonSignatureSerializer())
                            else -> throw SerializationException("Unknown type $type")
                        }
                    }

                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unknown index $index")
                }
            }
            when (type) {
                Secp256k1 -> SignatureWithPublicKey.Secp256k1(
                    publicKey = (publicKey as PublicKey.Secp256k1).v1,
                    signature = (signature as Signature.Secp256k1).value
                )

                Ed25519 -> SignatureWithPublicKey.Ed25519(
                    publicKey = (publicKey as PublicKey.Ed25519).v1,
                    signature = (signature as Signature.Ed25519).value
                )

                else -> throw SerializationException("Unknown type $type")
            }
        }
    }
}
