package com.babylon.wallet.android.data.repository.cache.database.accesscontroller

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.coreapi.ScryptoInstant
import com.radixdlt.sargon.AccessControllerAddress
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccessControllerEntity(
    @ColumnInfo("address")
    val address: AccessControllerAddress,
    val stateVersion: Long?,
    val allowTimedRecoveryAfter: ScryptoInstant?,
    val synced: Instant
)
