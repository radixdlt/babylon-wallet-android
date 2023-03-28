package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object NonFungibleResourcesCollectionItemSerializer :
    JsonContentPolymorphicSerializer<NonFungibleResourcesCollectionItem>(
        NonFungibleResourcesCollectionItem::class
    ) {

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<NonFungibleResourcesCollectionItem> {
        return when (element.jsonObject["aggregation_level"]?.jsonPrimitive?.content) {
            ResourceAggregationLevel.global.value -> NonFungibleResourcesCollectionItemGloballyAggregated.serializer()
            ResourceAggregationLevel.vault.value -> NonFungibleResourcesCollectionItemVaultAggregated.serializer()
            else -> error("")
        }
    }
}
