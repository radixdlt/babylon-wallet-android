package com.babylon.wallet.android.data.gateway.extensions

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.generated.models.MetadataValueType
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StringMetadataItem

fun StateNonFungibleDetailsResponseItem.name(): NameMetadataItem? = data
    ?.programmaticJson?.fields?.find { field ->
        field.field_name == ExplicitMetadataKey.NAME.key
    }?.value?.let { NameMetadataItem(name = it) }

fun StateNonFungibleDetailsResponseItem.image(): IconUrlMetadataItem? = data
    ?.programmaticJson?.fields?.find { field ->
        field.field_name == ExplicitMetadataKey.KEY_IMAGE_URL.key
    }?.value?.toUri()?.let { IconUrlMetadataItem(url = it) }

fun StateNonFungibleDetailsResponseItem.claimAmount(): ClaimAmountMetadataItem? = data
    ?.programmaticJson?.fields?.find { element ->
        element.kind == MetadataValueType.decimal.value
    }?.value?.toBigDecimalOrNull()?.let { claimAmount -> ClaimAmountMetadataItem(claimAmount) }

fun StateNonFungibleDetailsResponseItem.claimEpoch(): ClaimEpochMetadataItem? = data
    ?.programmaticJson?.fields?.find { element ->
        element.kind == MetadataValueType.u64.value
    }?.value?.toLongOrNull()?.let { ClaimEpochMetadataItem(it) }

fun StateNonFungibleDetailsResponseItem.stringMetadata(): List<StringMetadataItem>? = data?.programmaticJson?.fields
    ?.filterNot { field ->
        field.field_name == ExplicitMetadataKey.NAME.key || field.field_name == ExplicitMetadataKey.KEY_IMAGE_URL.key
    }?.mapNotNull { field ->
        val fieldName = field.field_name.orEmpty()
        val value = field.valueContent.orEmpty()
        if (fieldName.isNotEmpty() && value.isNotBlank()) {
            StringMetadataItem(fieldName, value)
        } else {
            null
        }
    }
