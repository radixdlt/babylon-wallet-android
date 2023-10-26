package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import java.math.BigDecimal
import java.time.Instant

data class AccountPortfolioResponse(
    @ColumnInfo("account_address")
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountTypeMetadataItem.AccountType?,
    @ColumnInfo("account_synced")
    val accountSynced: Instant,
    @ColumnInfo("account_state_version")
    val accountStateVersion: Long,

    // From AccountResourceJoin
    val amount: BigDecimal?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: String?,
    private val type: ResourceEntityType?,
    private val name: String?,
    private val symbol: String?,
    private val description: String?,
    @ColumnInfo("icon_url")
    private val iconUrl: String?,
    private val tags: TagsColumn?,
    @ColumnInfo("validator_address")
    private val validatorAddress: String?,
    @ColumnInfo("pool_address")
    private val poolAddress: String?,
    @ColumnInfo("dapp_definitions")
    private val dAppDefinitions: DappDefinitionsColumn?,
    private val divisibility: Int?,
    private val behaviours: BehavioursColumn?,
    private val supply: BigDecimal?,
    @ColumnInfo("synced")
    private val resourceSynced: Instant?
) {

    @Ignore
    val details: AccountDetails = AccountDetails(typeMetadataItem = accountType?.let { AccountTypeMetadataItem(it) })

    @Ignore
    val resource: ResourceEntity? = if (resourceAddress != null && type != null && resourceSynced != null) {
        ResourceEntity(
            address = resourceAddress,
            type = type,
            name = name,
            symbol = symbol,
            description = description,
            iconUrl = iconUrl,
            tags = tags,
            validatorAddress = validatorAddress,
            poolAddress = poolAddress,
            dAppDefinitions = dAppDefinitions,
            divisibility = divisibility,
            behaviours = behaviours,
            supply = supply,
            synced = resourceSynced
        )
    } else {
        null
    }

}


data class PoolWithResourceResponse(
    @ColumnInfo("pool_entity_address")
    val address: String,
    @ColumnInfo("pool_state_version")
    val stateVersion: Long?,
    val amount: BigDecimal?,

    // From ResourceEntity (@Embed does not work here since making ResourceEntity nullable does not work)
    @ColumnInfo("address")
    private val resourceAddress: String?,
    private val type: ResourceEntityType?,
    private val name: String?,
    private val symbol: String?,
    private val description: String?,
    @ColumnInfo("icon_url")
    private val iconUrl: String?,
    private val tags: TagsColumn?,
    @ColumnInfo("validator_address")
    private val validatorAddress: String?,
    @ColumnInfo("pool_address")
    private val poolAddress: String?,
    @ColumnInfo("dapp_definitions")
    private val dAppDefinitions: DappDefinitionsColumn?,
    private val divisibility: Int?,
    private val behaviours: BehavioursColumn?,
    private val supply: BigDecimal?,
    @ColumnInfo("synced")
    private val resourceSynced: Instant?
) {
    @Ignore
    val resource: ResourceEntity? = if (resourceAddress != null && type != null && resourceSynced != null) {
        ResourceEntity(
            address = resourceAddress,
            type = type,
            name = name,
            symbol = symbol,
            description = description,
            iconUrl = iconUrl,
            tags = tags,
            validatorAddress = validatorAddress,
            poolAddress = poolAddress,
            dAppDefinitions = dAppDefinitions,
            divisibility = divisibility,
            behaviours = behaviours,
            supply = supply,
            synced = resourceSynced
        )
    } else {
        null
    }
}

data class AccountOnNonFungibleCollectionStateResponse(
    @ColumnInfo("account_address")
    val accountAddress: String,
    @ColumnInfo("resource_address")
    val resourceAddress: String,
    @ColumnInfo("vault_address")
    val vaultAddress: String?,
    @ColumnInfo("vault_address")
    val nextCursor: String?,
    @ColumnInfo("state_version")
    val accountStateVersion: Long?
)
