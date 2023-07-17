package com.babylon.wallet.android.domain.model.metadata

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey

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

data class DomainMetadataItem(
    val domain: Uri
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.DOMAIN.key
}

data class DAppDefinitionsMetadataItem(
    val addresses: List<String> // TODO maybe change to component address
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.DAPP_DEFINITION.key
}

data class RelatedWebsiteMetadataItem(
    val website: String // TODO check that
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.RELATED_WEBSITES.key
}

data class ClaimedWebsiteMetadataItem(
    val website: String
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.CLAIMED_WEBSITES.key
}

data class ClaimedEntitiesMetadataItem(
    val entity: String
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
        ): KeyHash
        data class EddsaEd25519(
            override val hex: String
        ): KeyHash
    }
}
