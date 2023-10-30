package com.babylon.wallet.android.data.gateway.extensions

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValue
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueDecimal
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueString
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueTuple
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU64
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StringMetadataItem

fun StateNonFungibleDetailsResponseItem.asMetadataItems(): List<MetadataItem> {
    val fields = (data?.programmaticJson as? ProgrammaticScryptoSborValueTuple)?.fields ?: return emptyList()
    return fields.mapNotNull { it.asMetadataItem() }
}

private fun ProgrammaticScryptoSborValue.asMetadataItem(): MetadataItem? = when (this) {
    is ProgrammaticScryptoSborValueString -> when (val key = fieldName) {
        ExplicitMetadataKey.NAME.key -> NameMetadataItem(name = value)
        ExplicitMetadataKey.KEY_IMAGE_URL.key -> IconUrlMetadataItem(url = value.toUri())
        null -> null
        else -> StringMetadataItem(key,  value)
    }
    is ProgrammaticScryptoSborValueDecimal -> when (fieldName) {
        ExplicitMetadataKey.CLAIM_AMOUNT.key -> value.toBigDecimalOrNull()?.let { ClaimAmountMetadataItem(amount = it) }
        else -> null
    }
    is ProgrammaticScryptoSborValueU64 -> when (fieldName) {
        ExplicitMetadataKey.CLAIM_EPOCH.key -> value.toLongOrNull()?.let { ClaimEpochMetadataItem(claimEpoch = it) }
        else -> null
    }
    else -> null
}
