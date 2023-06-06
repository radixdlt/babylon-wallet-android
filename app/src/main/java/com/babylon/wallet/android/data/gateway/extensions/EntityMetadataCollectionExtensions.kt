package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem

fun EntityMetadataCollection.asMetadataStringMap() = items.associate { metadataItem ->
    metadataItem.key to metadataItem.value.asString
}.mapNotNull { (key, value) ->
    value?.let { key to it }
}.toMap()

fun EntityMetadataItem.toMetadataItem(): MetadataItem {
    val explicitMetadataKey = ExplicitMetadataKey.from(key = key)

    return explicitMetadataKey?.toStandardMetadataItem(value)
        ?: StringMetadataItem(
            key = key,
            value = value.asString.orEmpty()
        )
}

fun EntityMetadataCollection.asMetadataItems(): List<MetadataItem> {
    return items.map { item -> item.toMetadataItem() }
}
