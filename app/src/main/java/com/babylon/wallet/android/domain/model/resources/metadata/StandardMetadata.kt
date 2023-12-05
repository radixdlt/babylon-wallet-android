package com.babylon.wallet.android.domain.model.resources.metadata

import android.net.Uri
import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
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
)?.value

fun List<Metadata>.poolAddress(): String? = findPrimitive(
    key = ExplicitMetadataKey.POOL,
    type = MetadataType.Address
)?.value

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

fun List<Metadata>.claimedWebsites(): List<String>? =  findCollection(
    key = ExplicitMetadataKey.CLAIMED_WEBSITES,
    type = MetadataType.Url
)?.map { it.value }

fun List<Metadata>.claimedEntities(): List<String>? = findCollection(
    key = ExplicitMetadataKey.CLAIMED_ENTITIES,
    type = MetadataType.Address
)?.map { it.value }

fun List<Metadata>.ownerKeyHashes(): List<KeyHash>? = findCollection(
    key = ExplicitMetadataKey.OWNER_KEYS,
    type = MetadataType.PublicKeyHashEcdsaSecp256k1
)?.map { primitive ->
    KeyHash.EcdsaSecp256k1(primitive.value)
}?.let {
    val eddsa = findCollection(
        key = ExplicitMetadataKey.OWNER_KEYS,
        type = MetadataType.PublicKeyHashEddsaEd25519
    )?.map { primitive ->
        KeyHash.EddsaEd25519(primitive.value)
    }

    if (eddsa != null) {
        it + eddsa
    } else {
        it
    }
}

/**
 * Metadata items that are known to the wallet and are prominently presented.
 *
 * See the documentation [here](https://docs-babylon.radixdlt.com/main/standards/metadata-for-wallet-display.html)
 */
sealed interface StandardMetadataItem : MetadataItem

data class DescriptionMetadataItem(
    val description: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.DESCRIPTION.key
}

data class SymbolMetadataItem(
    val symbol: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.SYMBOL.key
}

data class NameMetadataItem(
    val name: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.NAME.key
}

data class DAppDefinitionsMetadataItem(
    val addresses: List<String> // TODO maybe change to component address
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.DAPP_DEFINITION.key
}

data class RelatedWebsitesMetadataItem(
    val websites: List<String>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.RELATED_WEBSITES.key
}

data class ClaimedWebsitesMetadataItem(
    val websites: List<String>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIMED_WEBSITES.key
}

data class ClaimedEntitiesMetadataItem(
    val entities: List<String>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIMED_ENTITIES.key
}

data class AccountTypeMetadataItem(
    val type: AccountType
) : StandardMetadataItem {

    override val key: String = ExplicitMetadataKey.ACCOUNT_TYPE.key

    companion object {
        fun from(value: String) = AccountType.values()
            .find { it.asString == value }
            ?.let {
                AccountTypeMetadataItem(it)
            }
    }
}

data class InfoUrlMetadataItem(
    val url: Uri
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.INFO_URL.key
}

data class IconUrlMetadataItem(
    val url: Uri
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.ICON_URL.key
}

data class TagsMetadataItem(
    val tags: List<String>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.TAGS.key
}

data class OwnerKeyHashesMetadataItem(
    val keyHashes: List<KeyHash>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.OWNER_KEYS.key

}

data class ValidatorMetadataItem(
    val validatorAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.VALIDATOR.key
}

data class ClaimAmountMetadataItem(
    val amount: BigDecimal
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIM_AMOUNT.key
}

data class ClaimEpochMetadataItem(
    val claimEpoch: Long
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIM_EPOCH.key
}

data class PoolMetadataItem(
    val poolAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.POOL.key
}

data class PoolUnitMetadataItem(
    val resourceAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.POOL_UNIT.key
}

data class ClaimNftMetadataItem(
    val stakeClaimNftAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIM_NFT.key
}
