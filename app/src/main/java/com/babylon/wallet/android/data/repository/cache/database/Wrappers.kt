package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import rdx.works.core.domain.resources.AccountDetails
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.metadata.AccountType
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
    @ColumnInfo("first_transaction_date")
    val firstTransactionDate: Instant?,
    // From AccountResourceJoin
    val amount: Decimal192?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: ResourceAddress?,
    private val type: ResourceEntityType?,
    @ColumnInfo("validator_address")
    private val validatorAddress: ValidatorAddress?,
    @ColumnInfo("pool_address")
    private val poolAddress: PoolAddress?,
    private val divisibility: Divisibility?,
    private val behaviours: BehavioursColumn?,
    private val supply: Decimal192?,
    private val metadata: MetadataColumn?,
    @ColumnInfo("synced")
    private val resourceSynced: Instant?
) {

    @Ignore
    val details: AccountDetails? = stateVersion?.let { version ->
        AccountDetails(
            stateVersion = version,
            accountType = accountType,
            firstTransactionDate = firstTransactionDate
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
    val address: PoolAddress,
    @ColumnInfo("pool_unit_address")
    val poolUnitAddress: ResourceAddress,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long?,
    @ColumnInfo("pool_metadata")
    val poolMetadata: MetadataColumn?,
    val amount: Decimal192?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: ResourceAddress?,
    private val type: ResourceEntityType?,
    @ColumnInfo("validator_address")
    private val validatorAddress: ValidatorAddress?,
    @ColumnInfo("pool_address")
    private val poolAddress: PoolAddress?,
    private val divisibility: Divisibility?,
    private val behaviours: BehavioursColumn?,
    private val supply: Decimal192?,
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

data class AccountStateVersion(
    @ColumnInfo("address")
    val address: AccountAddress,
    @ColumnInfo("state_version")
    val stateVersion: Long
)
