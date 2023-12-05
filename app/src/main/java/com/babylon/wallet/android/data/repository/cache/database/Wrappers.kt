package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import java.math.BigDecimal
import java.time.Instant

data class AccountPortfolioResponse(
    @ColumnInfo("account_address")
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountType?,
    @ColumnInfo("account_synced")
    val accountSynced: Instant?,
    @ColumnInfo("state_version")
    val stateVersion: Long?,

    // From AccountResourceJoin
    val amount: BigDecimal?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: String?,
    private val type: ResourceEntityType?,
    @ColumnInfo("validator_address")
    private val validatorAddress: String?,
    @ColumnInfo("pool_address")
    private val poolAddress: String?,
    private val divisibility: Int?,
    private val behaviours: BehavioursColumn?,
    private val supply: BigDecimal?,
    private val metadata: MetadataColumn?,
    @ColumnInfo("synced")
    private val resourceSynced: Instant?
) {

    @Ignore
    val details: AccountDetails? = stateVersion?.let { version ->
        AccountDetails(
            stateVersion = version,
            accountType = accountType
        )
    }

    @Ignore
    val resource: ResourceEntity? = if (resourceAddress != null && type != null && resourceSynced != null) {
        ResourceEntity(
            address = resourceAddress,
            type = type,
            validatorAddress = validatorAddress,
            poolAddress = poolAddress,
            divisibility = divisibility,
            behaviours = behaviours,
            supply = supply,
            metadata = metadata,
            synced = resourceSynced
        )
    } else {
        null
    }
}

data class PoolWithResourceResponse(
    @ColumnInfo("pool_entity_address")
    val address: String,
    @ColumnInfo("pool_unit_address")
    val poolUnitAddress: String,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long?,
    val amount: BigDecimal?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: String?,
    private val type: ResourceEntityType?,
    @ColumnInfo("validator_address")
    private val validatorAddress: String?,
    @ColumnInfo("pool_address")
    private val poolAddress: String?,
    private val divisibility: Int?,
    private val behaviours: BehavioursColumn?,
    private val supply: BigDecimal?,
    private val metadata: MetadataColumn?,
    @ColumnInfo("synced")
    private val resourceSynced: Instant?
) {
    @Ignore
    val resource: ResourceEntity? = if (resourceAddress != null && type != null && resourceSynced != null) {
        ResourceEntity(
            address = resourceAddress,
            type = type,
            validatorAddress = validatorAddress,
            poolAddress = poolAddress,
            divisibility = divisibility,
            behaviours = behaviours,
            supply = supply,
            metadata = metadata,
            synced = resourceSynced
        )
    } else {
        null
    }
}
