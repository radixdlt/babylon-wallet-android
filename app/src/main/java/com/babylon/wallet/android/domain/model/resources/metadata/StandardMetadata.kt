@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.domain.model.resources.metadata

import android.net.Uri
import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.utils.toAddressOrNull
import com.radixdlt.ret.EntityType.GLOBAL_MULTI_RESOURCE_POOL
import com.radixdlt.ret.EntityType.GLOBAL_ONE_RESOURCE_POOL
import com.radixdlt.ret.EntityType.GLOBAL_TWO_RESOURCE_POOL
import com.radixdlt.ret.EntityType.GLOBAL_VALIDATOR
import java.math.BigDecimal

private fun List<Metadata>.findPrimitive(key: ExplicitMetadataKey, type: MetadataType): Metadata.Primitive? = find {
    it.key == key.key && (it as? Metadata.Primitive)?.valueType == type
} as? Metadata.Primitive

private fun List<Metadata>.findCollection(key: ExplicitMetadataKey, type: MetadataType): List<Metadata.Primitive>? = find {
    it.key == key.key
}?.let { metadata ->
    (metadata as? Metadata.Collection)
        ?.values
        ?.filterIsInstance<Metadata.Primitive>()
        ?.takeIf { values -> values.all { it.valueType == type } }
}

fun List<Metadata>.description(): String? = findPrimitive(
    key = ExplicitMetadataKey.DESCRIPTION,
    type = MetadataType.String
)?.value

fun List<Metadata>.symbol(): String? = findPrimitive(
    key = ExplicitMetadataKey.SYMBOL,
    type = MetadataType.String
)?.value

fun List<Metadata>.name(): String? = findPrimitive(
    key = ExplicitMetadataKey.NAME,
    type = MetadataType.String
)?.value

fun List<Metadata>.infoUrl(): Uri? = findPrimitive(
    key = ExplicitMetadataKey.INFO_URL,
    type = MetadataType.Url
)?.value?.toUri()

fun List<Metadata>.iconUrl(): Uri? = findPrimitive(
    key = ExplicitMetadataKey.ICON_URL,
    type = MetadataType.Url
)?.value?.toUri()

fun List<Metadata>.keyImageUrl(): Uri? = findPrimitive(
    key = ExplicitMetadataKey.KEY_IMAGE_URL,
    type = MetadataType.Url
)?.value?.toUri()

fun List<Metadata>.validatorAddress(): String? = findPrimitive(
    key = ExplicitMetadataKey.VALIDATOR,
    type = MetadataType.Address
)?.value?.takeIf { value ->
    value.toAddressOrNull()?.entityType() == GLOBAL_VALIDATOR
}

fun List<Metadata>.poolAddress(): String? = findPrimitive(
    key = ExplicitMetadataKey.POOL,
    type = MetadataType.Address
)?.value?.takeIf { value ->
    value.toAddressOrNull()?.entityType() in setOf(GLOBAL_ONE_RESOURCE_POOL, GLOBAL_TWO_RESOURCE_POOL, GLOBAL_MULTI_RESOURCE_POOL)
}

fun List<Metadata>.ownerBadge(): OwnerBadge? = findPrimitive(
    key = ExplicitMetadataKey.OWNER_BADGE,
    type = MetadataType.NonFungibleLocalId
)?.let { OwnerBadge(it.value, it.lastUpdatedAtStateVersion) }

fun List<Metadata>.poolUnit(): String? = findPrimitive(
    key = ExplicitMetadataKey.POOL_UNIT,
    type = MetadataType.Address
)?.value

fun List<Metadata>.claimAmount(): BigDecimal? = findPrimitive(
    key = ExplicitMetadataKey.CLAIM_AMOUNT,
    type = MetadataType.Decimal
)?.value?.toBigDecimalOrNull()

fun List<Metadata>.claimEpoch(): Long? = findPrimitive(
    key = ExplicitMetadataKey.CLAIM_EPOCH,
    type = MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
)?.value?.toLongOrNull()

fun List<Metadata>.claimNFTAddress(): String? = findPrimitive(
    key = ExplicitMetadataKey.CLAIM_NFT,
    type = MetadataType.Address
)?.value

fun List<Metadata>.accountType(): AccountType? = findPrimitive(
    key = ExplicitMetadataKey.ACCOUNT_TYPE,
    type = MetadataType.String
)?.value?.let { AccountType.from(it) }

fun List<Metadata>.tags(): List<String>? = findCollection(
    key = ExplicitMetadataKey.TAGS,
    type = MetadataType.String
)?.map { it.value }

fun List<Metadata>.dAppDefinitions(): List<String>? = findCollection(
    key = ExplicitMetadataKey.DAPP_DEFINITIONS,
    type = MetadataType.Address
)?.map { it.value }?.let { dAppDefinitions ->
    val value = findPrimitive(
        key = ExplicitMetadataKey.DAPP_DEFINITION,
        type = MetadataType.Address
    )?.value

    if (value != null) {
        dAppDefinitions + value
    } else {
        dAppDefinitions
    }
}

fun List<Metadata>.relatedWebsites(): List<String>? = findCollection(
    key = ExplicitMetadataKey.RELATED_WEBSITES,
    type = MetadataType.Url
)?.map { it.value }

fun List<Metadata>.claimedWebsites(): List<String>? = findCollection(
    key = ExplicitMetadataKey.CLAIMED_WEBSITES,
    type = MetadataType.Url
)?.map { it.value }

fun List<Metadata>.claimedEntities(): List<String>? = findCollection(
    key = ExplicitMetadataKey.CLAIMED_ENTITIES,
    type = MetadataType.Address
)?.map { it.value }

fun List<Metadata>.ownerKeyHashes(): PublicKeyHashes? = findCollection(
    key = ExplicitMetadataKey.OWNER_KEYS,
    type = MetadataType.PublicKeyHashEcdsaSecp256k1
)?.map { primitive ->
    PublicKeyHash.EcdsaSecp256k1(primitive.value, primitive.lastUpdatedAtStateVersion)
}?.let {
    val eddsa = findCollection(
        key = ExplicitMetadataKey.OWNER_KEYS,
        type = MetadataType.PublicKeyHashEddsaEd25519
    )?.map { primitive ->
        PublicKeyHash.EddsaEd25519(primitive.value, primitive.lastUpdatedAtStateVersion)
    }
    val keys = if (eddsa != null) {
        it + eddsa
    } else {
        it
    }
    PublicKeyHashes(
        keys = keys,
        lastUpdatedAtStateVersion = keys.firstOrNull()?.lastUpdatedAtStateVersion ?: 0L
    )
}
