package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import java.math.BigDecimal

sealed class Resource {
    abstract val resourceAddress: String

    data class FungibleResource(
        override val resourceAddress: String,
        val amount: BigDecimal,
        private val nameMetadataItem: NameMetadataItem? = null,
        private val symbolMetadataItem: SymbolMetadataItem? = null,
        private val descriptionMetadataItem: DescriptionMetadataItem? = null,
        private val iconUrlMetadataItem: IconUrlMetadataItem? = null,
    ): Resource() {
        val name: String?
            get() = nameMetadataItem?.name // .orEmpty()

        val symbol: String
            get() = symbolMetadataItem?.symbol.orEmpty()

        val isXrd: Boolean
            get() = symbolMetadataItem?.isXrd ?: false

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        val iconUrl: Uri?
            get() = iconUrlMetadataItem?.url

        val displayTitle: String
            get() = if (symbol.isNotBlank()) {
                symbol
            } else if (name?.isNotBlank() == true) {
                name.orEmpty()
            } else {
                ""
            }
    }

    data class NonFungibleResource(
        override val resourceAddress: String,
        val amount: Long,
        private val nameMetadataItem: NameMetadataItem? = null,
        private val descriptionMetadataItem: DescriptionMetadataItem? = null,
        private val iconMetadataItem: IconUrlMetadataItem? = null,
        val items: List<Item>
    ): Resource() {
        val name: String
            get() = nameMetadataItem?.name.orEmpty()

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        val iconUrl: Uri?
            get() = iconMetadataItem?.url



        data class Item(
            val localId: String,
            val iconMetadataItem: IconUrlMetadataItem?
        ) {

            val imageUrl: Uri?
                get() = iconMetadataItem?.url

            fun globalAddress(nftAddress: String) = "$nftAddress:$localId"

        }
    }

}
