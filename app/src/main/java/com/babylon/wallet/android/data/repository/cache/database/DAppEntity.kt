package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.DApp
import java.time.Instant

@Entity
data class DAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "definition_address")
    val definitionAddress: String,
    val metadata: MetadataColumn?,
    val synced: Instant
) {

    fun toDApp() = DApp(
        dAppAddress = AccountAddress.init(definitionAddress),
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun from(item: StateEntityDetailsResponseItem, syncedAt: Instant) = DAppEntity(
            definitionAddress = item.address,
            metadata = item.explicitMetadata?.toMetadata()?.let { MetadataColumn(it) },
            synced = syncedAt
        )
    }
}
