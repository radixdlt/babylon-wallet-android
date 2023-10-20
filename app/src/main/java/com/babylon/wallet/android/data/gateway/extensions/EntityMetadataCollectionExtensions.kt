package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem

fun EntityMetadataItem.toMetadataItem(): MetadataItem? {
    val explicitMetadataKey = ExplicitMetadataKey.from(key = key)
    return explicitMetadataKey?.toStandardMetadataItem(value)
}

fun EntityMetadataCollection.asMetadataItems(): List<MetadataItem> {
    return items.mapNotNull { item -> item.toMetadataItem() }
}
