package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.DeserializationStrategy
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
                if (requestVersion != WalletInteraction.Metadata.VERSION) {
                    throw IncompatibleRequestVersionException(requestId, requestVersion)
                }
                WalletInteraction.serializer()
            }

            else -> LedgerInteractionResponse.serializer()
        }
    }
}

class IncompatibleRequestVersionException(
    val requestId: String,
    val requestVersion: Long?
) : IllegalStateException()

val peerdroidRequestJson = Json {
    classDiscriminator = "discriminator"
}
