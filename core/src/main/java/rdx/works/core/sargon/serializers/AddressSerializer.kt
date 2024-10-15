package rdx.works.core.sargon.serializers

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class AddressSerializer : KSerializer<Address> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.Address", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Address) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): Address {
        return Address.init(decoder.decodeString())
    }
}

class IdentityAddressSerializer : KSerializer<IdentityAddress> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.IdentityAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IdentityAddress) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): IdentityAddress {
        return IdentityAddress.init(decoder.decodeString())
    }
}

class AccountAddressSerializer : KSerializer<AccountAddress> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("com.radixdlt.sargon.AccountAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AccountAddress) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): AccountAddress {
        return AccountAddress.init(decoder.decodeString())
    }
}

class AddressOfAccountOrPersonaSerializer : KSerializer<AddressOfAccountOrPersona> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.radixdlt.sargon.AddressOfAccountOrPersona", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AddressOfAccountOrPersona) {
        encoder.encodeString(value.string)
    }

    override fun deserialize(decoder: Decoder): AddressOfAccountOrPersona {
        return AddressOfAccountOrPersona.init(decoder.decodeString())
    }
}
