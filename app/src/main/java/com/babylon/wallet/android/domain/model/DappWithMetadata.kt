package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem

data class DappWithMetadata(
    val dAppDefinitionAddress: String,
    private val nameItem: NameMetadataItem? = null,
    private val descriptionItem: DescriptionMetadataItem? = null,
    private val iconMetadataItem: IconUrlMetadataItem? = null,
    private val relatedWebsitesItem: RelatedWebsiteMetadataItem? = null,
    private val accountTypeItem: AccountTypeMetadataItem? = null,
    private val dAppDefinitionMetadataItem: DAppDefinitionMetadataItem? = null,
    private val nonExplicitMetadataItems: List<StringMetadataItem> = emptyList()
) {

    val name: String?
        get() = nameItem?.name

    val description: String?
        get() = descriptionItem?.description

    val iconUrl: Uri?
        get() = iconMetadataItem?.url

    val isDappDefinition: Boolean
        get() = accountTypeItem?.type == AccountTypeMetadataItem.AccountType.DAPP_DEFINITION

    val displayableMetadata: List<StringMetadataItem>
        get() = nonExplicitMetadataItems

    val definitionAddress: String?
        get() = dAppDefinitionMetadataItem?.address

    fun isRelatedWith(origin: String): Boolean {
        return relatedWebsitesItem?.website == origin
    }

    companion object {
        fun from(address: String, metadataItems: List<MetadataItem> = listOf()): DappWithMetadata {
            val remainingItems = metadataItems.toMutableList()

            return DappWithMetadata(
                dAppDefinitionAddress = address,
                nameItem = remainingItems.consume(),
                descriptionItem = remainingItems.consume(),
                iconMetadataItem = remainingItems.consume(),
                relatedWebsitesItem = remainingItems.consume(),
                accountTypeItem = remainingItems.consume(),
                nonExplicitMetadataItems = remainingItems.filterIsInstance<StringMetadataItem>()
            )
        }

        private inline fun <reified T: MetadataItem> MutableList<MetadataItem>.consume(): T? {
            val item = find { it is T } as? T

            if (item != null) {
                remove(item)
            }

            return item
        }
    }
}
