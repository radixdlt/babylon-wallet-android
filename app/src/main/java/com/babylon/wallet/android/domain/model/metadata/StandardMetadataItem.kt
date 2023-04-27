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

    val isXrd: Boolean
        get() = symbol == KnownSymbol.XRD

    override val key: String = ExplicitMetadataKey.SYMBOL.key

    private object KnownSymbol {
        const val XRD = "XRD"
    }
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

data class DAppDefinitionMetadataItem(
    val address: String // TODO maybe change to component address
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.DAPP_DEFINITION.key
}

data class RelatedWebsiteMetadataItem(
    val website: String // TODO check that
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.RELATED_WEBSITES.key
}

data class AccountTypeMetadataItem(
    val type: AccountType
) : StandardMetadataItem {

    override val key: String = ExplicitMetadataKey.ACCOUNT_TYPE.key

    enum class AccountType(val asString: String) {
        DAPP_DEFINITION("dapp definition")
    }

}

data class IconUrlMetadataItem(
    val url: Uri
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.ICON_URL.key
}
