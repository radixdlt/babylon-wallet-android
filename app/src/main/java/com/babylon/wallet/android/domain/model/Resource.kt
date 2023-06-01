package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.KnownEntityAddressesRequest
import rdx.works.profile.data.model.apppreferences.Radix
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
    ) : Resource(), Comparable<FungibleResource> {
        val name: String?
            get() = nameMetadataItem?.name // .orEmpty()

        val symbol: String
            get() = symbolMetadataItem?.symbol.orEmpty()

        val isXrd: Boolean
            get() = RadixEngineToolkit.knownEntityAddresses(
                KnownEntityAddressesRequest(networkId = Radix.Gateway.default.network.id.toUByte())
            ).getOrNull()?.xrdResourceAddress == resourceAddress

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

        override fun compareTo(other: FungibleResource): Int {
            compareValuesBy(
                this,
                other,
                { it.amount },
                { it.name }
            )
            val symbolDiff = symbol.compareTo(other.symbol)
            if (symbolDiff == 0) {

            }
            if (symbol.isNotBlank() && other.symbol.isBlank()) {
                return 1
            } else if (symbol.isBlank() && other.symbol.isNotBlank()) {
                return -1
            } else if (symbol.isNotBlank() && other.symbol.isNotBlank()) {
                return if (symbol == other.symbol) {
                    resourceAddress.compareTo(other.resourceAddress)
                } else {
                    symbol.compareTo(other.symbol)
                }
            }



            return resourceAddress.compareTo(other.resourceAddress)
        }
    }

    data class NonFungibleResource(
        override val resourceAddress: String,
        val amount: Long,
        private val nameMetadataItem: NameMetadataItem? = null,
        private val descriptionMetadataItem: DescriptionMetadataItem? = null,
        private val iconMetadataItem: IconUrlMetadataItem? = null,
        val items: List<Item>
    ) : Resource() {
        val name: String
            get() = nameMetadataItem?.name.orEmpty()

        val description: String
            get() = descriptionMetadataItem?.description.orEmpty()

        val iconUrl: Uri?
            get() = iconMetadataItem?.url

        data class Item(
            val collectionAddress: String,
            val localId: String,
            val iconMetadataItem: IconUrlMetadataItem?
        ) {

            val imageUrl: Uri?
                get() = iconMetadataItem?.url

            val globalAddress: String
                get() = "$collectionAddress:$localId"
        }
    }
}
