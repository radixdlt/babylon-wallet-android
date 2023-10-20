package com.babylon.wallet.android.domain.model.resources.metadata

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey

/**
 * Generic [MetadataItem] that the [key] is not included in the
 * [ExplicitMetadataKey]s and whose [value] is represented as a [String]
 */
data class StringMetadataItem(
    override val key: String,
    val value: String
) : MetadataItem
