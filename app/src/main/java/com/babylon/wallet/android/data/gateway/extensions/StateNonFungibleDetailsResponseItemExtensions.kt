package com.babylon.wallet.android.data.gateway.extensions

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValue
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueArray
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueBool
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueBytes
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueDecimal
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueEnum
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI128
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI16
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI32
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI64
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueI8
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueMap
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueNonFungibleLocalId
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueOwn
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValuePreciseDecimal
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueReference
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueString
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueTuple
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU128
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU16
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU32
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU64
import com.babylon.wallet.android.data.gateway.generated.models.ProgrammaticScryptoSborValueU8
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimAmountMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ClaimEpochMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.ComplexMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.StringMetadataItem

fun StateNonFungibleDetailsResponseItem.asMetadataItems(): List<MetadataItem> {
    val fields = (data?.programmaticJson as? ProgrammaticScryptoSborValueTuple)?.fields ?: return emptyList()
    return fields.mapNotNull { it.asMetadataItem() }
}

@Suppress("CyclomaticComplexMethod")
private fun ProgrammaticScryptoSborValue.asMetadataItem(): MetadataItem? = when (this) {
    is ProgrammaticScryptoSborValueString -> when (val key = fieldName) {
        ExplicitMetadataKey.NAME.key -> NameMetadataItem(name = value)
        ExplicitMetadataKey.KEY_IMAGE_URL.key -> IconUrlMetadataItem(url = value.toUri())
        null -> null
        else -> StringMetadataItem(key, value)
    }
    is ProgrammaticScryptoSborValueDecimal -> when (val key = fieldName) {
        ExplicitMetadataKey.CLAIM_AMOUNT.key -> value.toBigDecimalOrNull()?.let { ClaimAmountMetadataItem(amount = it) }
        null -> null
        else -> StringMetadataItem(key, value)
    }
    is ProgrammaticScryptoSborValueU64 -> when (val key = fieldName) {
        ExplicitMetadataKey.CLAIM_EPOCH.key -> value.toLongOrNull()?.let { ClaimEpochMetadataItem(claimEpoch = it) }
        null -> null
        else -> StringMetadataItem(key, value)
    }
    is ProgrammaticScryptoSborValueBool ->  fieldName?.let { StringMetadataItem(it, value.toString()) }
    is ProgrammaticScryptoSborValueI8 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueI16 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueI32 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueI64 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueI128 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueU8 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueU16 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueU32 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueU128 -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueEnum -> fieldName?.let { StringMetadataItem(it, typeName.orEmpty()) }
    is ProgrammaticScryptoSborValueBytes -> fieldName?.let { StringMetadataItem(it, hex) }
    is ProgrammaticScryptoSborValueArray -> fieldName?.let { ComplexMetadataItem(it) }
    is ProgrammaticScryptoSborValueMap -> fieldName?.let { ComplexMetadataItem(it) }
    is ProgrammaticScryptoSborValueTuple -> fieldName?.let { ComplexMetadataItem(it) }
    is ProgrammaticScryptoSborValueReference -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueOwn -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValuePreciseDecimal -> fieldName?.let { StringMetadataItem(it, value) }
    is ProgrammaticScryptoSborValueNonFungibleLocalId -> fieldName?.let { StringMetadataItem(it, value) }
    else -> null
}
