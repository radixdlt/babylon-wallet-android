package com.babylon.wallet.android.domain.model.metadata

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import rdx.works.profile.data.model.pernetwork.FactorInstance

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

data class OwnerKeysMetadataItem(
    val ownerKeys: List<String>
) : StandardMetadataItem {
    override val key: String = ExplicitMetadataKey.OWNER_KEYS.key

    @Suppress("MagicNumber")
    fun toPublicKeys(): List<FactorInstance.PublicKey> {
        val curve25519Prefix = "EddsaEd25519PublicKey"
        val secp256k1Prefix = "EcdsaSecp256k1PublicKey"
        val lengthCurve25519Prefix = curve25519Prefix.length
        val lengthSecp256k1Prefix = secp256k1Prefix.length
        val lengthQuoteAndParenthesis = 2
        val lengthQuotesAndTwoParenthesis = 2 * lengthQuoteAndParenthesis
        val lengthCurve25519PubKeyHex = 32 * 2
        val lengthSecp256K1PubKeyHex = 33 * 2
        val slip10Keys = ownerKeys.map { ownerKey ->
            when {
                ownerKey.startsWith(curve25519Prefix) -> {
                    require(ownerKey.length == lengthQuotesAndTwoParenthesis + lengthCurve25519Prefix + lengthCurve25519PubKeyHex)
                    val keyHex = ownerKey.drop(lengthQuoteAndParenthesis + lengthCurve25519Prefix).dropLast(lengthQuoteAndParenthesis)
                    FactorInstance.PublicKey.curve25519PublicKey(keyHex)
                }
                else -> {
                    require(ownerKey.length == lengthQuotesAndTwoParenthesis + lengthSecp256k1Prefix + lengthSecp256K1PubKeyHex)
                    val keyHex = ownerKey.drop(lengthQuoteAndParenthesis + lengthSecp256k1Prefix).dropLast(lengthQuoteAndParenthesis)
                    FactorInstance.PublicKey.curveSecp256k1PublicKey(keyHex)
                }
            }
        }
        return slip10Keys
    }
}
