package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.AccessRulesChain
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

object AccessRulesChainSerializer : JsonTransformingSerializer<List<AccessRulesChain>>(ListSerializer(AccessRulesChain.serializer())) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (element is JsonArray) {
            element
        } else {
            JsonArray(listOf(element))
        }
    }
}
