package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedEntitiesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsiteMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem

data class DAppWithMetadata(
    val dAppAddress: String,
    private val nameItem: NameMetadataItem? = null,
    private val descriptionItem: DescriptionMetadataItem? = null,
    private val iconMetadataItem: IconUrlMetadataItem? = null,
    private val relatedWebsitesItem: RelatedWebsiteMetadataItem? = null,
    val claimedWebsiteItem: ClaimedWebsiteMetadataItem? = null,
    private val claimedEntitiesItem: ClaimedEntitiesMetadataItem? = null,
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

    val claimedWebsite: String?
        get() = claimedWebsiteItem?.website

    val claimedEntities: List<String>
        get() = claimedEntitiesItem?.entity?.split(",")?.map { it.trim() }.orEmpty()

    fun isRelatedWith(origin: String): Boolean {
        return relatedWebsitesItem?.website == origin
    }

    companion object {
        fun from(address: String, metadataItems: List<MetadataItem> = listOf()): DAppWithMetadata {
            val remainingItems = metadataItems.toMutableList()

            return DAppWithMetadata(
                dAppAddress = address,
                nameItem = remainingItems.consume(),
                descriptionItem = remainingItems.consume(),
                iconMetadataItem = remainingItems.consume(),
                relatedWebsitesItem = remainingItems.consume(),
                claimedWebsiteItem = remainingItems.consume(),
                claimedEntitiesItem = remainingItems.consume(),
                accountTypeItem = remainingItems.consume(),
                nonExplicitMetadataItems = remainingItems.filterIsInstance<StringMetadataItem>()
            )
        }
    }
}
