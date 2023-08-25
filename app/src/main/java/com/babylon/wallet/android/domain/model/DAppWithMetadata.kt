package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.AccountTypeMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedEntitiesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.ClaimedWebsitesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DAppDefinitionsMetadataItem
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.RelatedWebsitesMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

data class DAppWithMetadata(
    val dAppAddress: String,
    private val nameItem: NameMetadataItem? = null,
    private val descriptionItem: DescriptionMetadataItem? = null,
    private val iconMetadataItem: IconUrlMetadataItem? = null,
    private val relatedWebsitesItem: RelatedWebsitesMetadataItem? = null,
    private val claimedWebsitesItem: ClaimedWebsitesMetadataItem? = null,
    private val claimedEntitiesItem: ClaimedEntitiesMetadataItem? = null,
    private val accountTypeItem: AccountTypeMetadataItem? = null,
    private val dAppDefinitionsMetadataItem: DAppDefinitionsMetadataItem? = null,
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

    val definitionAddresses: List<String>
        get() = dAppDefinitionsMetadataItem?.addresses.orEmpty()

    val claimedWebsites: ImmutableList<String>
        get() = claimedWebsitesItem?.websites.orEmpty().toPersistentList()

    val claimedEntities: List<String>
        get() = claimedEntitiesItem?.entities.orEmpty()

    @Suppress("SwallowedException")
    fun isRelatedWith(origin: String): Boolean {
        return claimedWebsites.any {
            try {
                val claimedUri = Uri.parse(it)
                val originUri = Uri.parse(origin)
                claimedUri.scheme != null && claimedUri.scheme == originUri.scheme
            } catch (e: Exception) {
                false
            }
        }
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
                claimedWebsitesItem = remainingItems.consume(),
                claimedEntitiesItem = remainingItems.consume(),
                dAppDefinitionsMetadataItem = remainingItems.consume(),
                accountTypeItem = remainingItems.consume(),
                nonExplicitMetadataItems = remainingItems.filterIsInstance<StringMetadataItem>()
            )
        }
    }
}
