package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
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
            ResourceAggregationLevel.Global.value -> FungibleResourcesCollectionItemGloballyAggregated.serializer()
            ResourceAggregationLevel.Vault.value -> FungibleResourcesCollectionItemVaultAggregated.serializer()
            else -> error("")
        }
    }
}
