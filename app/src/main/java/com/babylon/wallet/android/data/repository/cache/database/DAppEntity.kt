package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babylon.wallet.android.data.gateway.extensions.toMetadata
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.metadata.lockerAddress
import java.time.Instant

@Entity
data class DAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "definition_address")
    val definitionAddress: AccountAddress,
    val metadata: MetadataColumn?,
    val synced: Instant,
    val lockerAddress: LockerAddress?
) {

    fun toDApp() = DApp(
        dAppAddress = definitionAddress,
        lockerAddress = lockerAddress,
        metadata = metadata?.metadata.orEmpty()
    )

    companion object {
        fun from(item: StateEntityDetailsResponseItem, syncedAt: Instant) = DAppEntity(
            definitionAddress = AccountAddress.init(item.address),
            metadata = MetadataColumn.from(
                explicitMetadata = item.explicitMetadata,
                implicitMetadata = item.metadata
            ),
            synced = syncedAt,
            lockerAddress = item.metadata.toMetadata().lockerAddress()
        )
    }
}
