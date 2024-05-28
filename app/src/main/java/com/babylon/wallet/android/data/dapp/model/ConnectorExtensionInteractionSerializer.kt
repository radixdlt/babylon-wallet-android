package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object ConnectorExtensionInteractionSerializer :
    JsonContentPolymorphicSerializer<ConnectorExtensionInteraction>(ConnectorExtensionInteraction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ConnectorExtensionInteraction> {
        return when {
            element.jsonObject["items"] != null -> {
                // check for incoming request version compatibility: WalletInteraction.Metadata.VERSION
                val requestId = element.jsonObject["interactionId"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val requestVersion = element.jsonObject["metadata"]?.jsonObject?.get("version")?.jsonPrimitive?.longOrNull
//                if (requestVersion != WalletInteraction.Metadata.VERSION) {
//                    throw IncompatibleRequestVersionException(requestId, requestVersion)
//                }
                DappToWalletInteractionUnvalidatedSerializer
            }

            else -> LedgerInteractionResponse.serializer()
        }
    }
}

object DappToWalletInteractionUnvalidatedSerializer : KSerializer<ConnectorExtensionInteraction> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("DappToWalletInteractionUnvalidated", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ConnectorExtensionInteraction {
        val jsonString = decoder.decodeString()
        return sargonDappToWalletInteractionUnvalidated(jsonString.toByteArray())
    }

    override fun serialize(encoder: Encoder, value: ConnectorExtensionInteraction) {
        val encodedString = sargonDappToWalletInteractionUnvalidated(value as DappToWalletInteractionUnvalidated)
        encoder.encodeString(encodedString)
    }

}

fun sargonDappToWalletInteractionUnvalidated(value: ByteArray): ConnectorExtensionInteraction {
    TODO("Not implemented")
}

fun sargonDappToWalletInteractionUnvalidated(value: DappToWalletInteractionUnvalidated): String {
    TODO("Not implemented")
}


class IncompatibleRequestVersionException(
    val requestId: String,
    val requestVersion: Long?
) : IllegalStateException()

val peerdroidRequestJson = Json {
    classDiscriminator = "discriminator"
}
