package com.babylon.wallet.android.domain.model.resources.metadata

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import java.math.BigDecimal

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

    enum class AccountType(val asString: String) {
        DAPP_DEFINITION("dapp definition")
    }

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

    sealed interface KeyHash {
        val hex: String

        data class EcdsaSecp256k1(
            override val hex: String
        ) : KeyHash
        data class EddsaEd25519(
            override val hex: String
        ) : KeyHash
    }
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
    val poolAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.POOL_UNIT.key
}

data class ClaimNftMetadataItem(
    val stakeClaimNftAddress: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIM_NFT.key
}
