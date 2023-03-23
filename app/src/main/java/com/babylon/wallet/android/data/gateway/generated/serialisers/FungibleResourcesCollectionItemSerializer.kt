package com.babylon.wallet.android.data.gateway.generated.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object FungibleResourcesCollectionItemSerializer :
    JsonContentPolymorphicSerializer<FungibleResourcesCollectionItem>(
        FungibleResourcesCollectionItem::class
    ) {

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<FungibleResourcesCollectionItem> {
        return when (element.jsonObject["aggregation_level"]?.jsonPrimitive?.content) {
            ResourceAggregationLevel.global.value -> FungibleResourcesCollectionItemGloballyAggregated.serializer()
            ResourceAggregationLevel.vault.value -> FungibleResourcesCollectionItemVaultAggregated.serializer()
            else -> error("")
        }
    }
}
