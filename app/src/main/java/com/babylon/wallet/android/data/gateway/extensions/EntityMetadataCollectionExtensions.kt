package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection

fun EntityMetadataCollection.asMetadataStringMap() = items.associate { metadataItem ->
    metadataItem.key to metadataItem.value.asString
}.mapNotNull { (key, value) ->
    value?.let { key to it }
}.toMap()
