package com.babylon.wallet.android.data.gateway.serialisers

import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersCollectionFungibleResourceItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersCollectionNonFungibleResourceItem
import com.babylon.wallet.android.data.gateway.generated.models.ResourceHoldersResourceType
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ResourceHoldersCollectionItemSerializer : JsonContentPolymorphicSerializer<ResourceHoldersCollectionItem>(
    ResourceHoldersCollectionItem::class
) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<ResourceHoldersCollectionItem> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            ResourceHoldersResourceType.FungibleResource.value -> ResourceHoldersCollectionFungibleResourceItem.serializer()
            ResourceHoldersResourceType.NonFungibleResource.value -> ResourceHoldersCollectionNonFungibleResourceItem.serializer()
            else -> error("")
        }
    }
}
