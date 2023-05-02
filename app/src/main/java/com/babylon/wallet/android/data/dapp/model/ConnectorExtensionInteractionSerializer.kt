package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object ConnectorExtensionInteractionSerializer :
    JsonContentPolymorphicSerializer<ConnectorExtensionInteraction>(ConnectorExtensionInteraction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ConnectorExtensionInteraction> {
        return when {
            element.jsonObject["items"] != null -> WalletInteraction.serializer()
            else -> LedgerInteractionResponse.serializer()
        }
    }
}

val peerdroidRequestJson = Json {
    classDiscriminator = "discriminator"
}
