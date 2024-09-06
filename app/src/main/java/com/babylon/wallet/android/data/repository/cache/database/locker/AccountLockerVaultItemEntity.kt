package com.babylon.wallet.android.data.repository.cache.database.locker

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItem
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemFungible
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemNonFungible
import com.babylon.wallet.android.data.gateway.generated.models.AccountLockerVaultCollectionItemType
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountLockerClaimableResource
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.SargonException
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toDecimal192

@Entity(primaryKeys = ["locker_address", "account_address", "resource_address", "amount", "total_count"])
data class AccountLockerVaultItemEntity(
    @ColumnInfo(name = "locker_address")
    val lockerAddress: LockerAddress,
    @ColumnInfo(name = "account_address")
    val accountAddress: AccountAddress,
    @ColumnInfo(name = "resource_address")
    val resourceAddress: ResourceAddress,
    @ColumnInfo(name = "vault_address")
    val vaultAddress: VaultAddress,
    @ColumnInfo(name = "last_updated_at_state_version")
    val lastUpdatedAtStateVersion: Long,
    @ColumnInfo(name = "is_fungible")
    val isFungible: Boolean,
    @ColumnInfo(name = "amount")
    val amount: Decimal192,
    @ColumnInfo(name = "total_count")
    val totalCount: Long
) {

    @Throws(SargonException::class)
    fun into(): AccountLockerClaimableResource {
        return if (isFungible) {
            AccountLockerClaimableResource.Fungible(
                resourceAddress = resourceAddress,
                amount = amount
            )
        } else {
            AccountLockerClaimableResource.NonFungible(
                resourceAddress = resourceAddress,
                numberOfItems = totalCount.toULong()
            )
        }
    }

    companion object {

        @Throws(SargonException::class)
        fun from(
            item: AccountLockerVaultCollectionItem,
            accountAddress: AccountAddress,
            lockerAddress: LockerAddress
        ): AccountLockerVaultItemEntity? {
            return when (item) {
                is AccountLockerVaultCollectionItemFungible -> {
                    val amount = item.amount.toDecimal192()
                    AccountLockerVaultItemEntity(
                        lockerAddress = lockerAddress,
                        accountAddress = accountAddress,
                        resourceAddress = ResourceAddress.init(item.resourceAddress),
                        vaultAddress = VaultAddress.init(item.vaultAddress),
                        lastUpdatedAtStateVersion = item.lastUpdatedAtStateVersion,
                        isFungible = item.type == AccountLockerVaultCollectionItemType.Fungible,
                        amount = amount,
                        totalCount = 0
                    ).takeIf { amount > 0.toDecimal192() }
                }
                is AccountLockerVaultCollectionItemNonFungible -> {
                    AccountLockerVaultItemEntity(
                        lockerAddress = lockerAddress,
                        accountAddress = accountAddress,
                        resourceAddress = ResourceAddress.init(item.resourceAddress),
                        vaultAddress = VaultAddress.init(item.vaultAddress),
                        lastUpdatedAtStateVersion = item.lastUpdatedAtStateVersion,
                        isFungible = item.type == AccountLockerVaultCollectionItemType.Fungible,
                        amount = 0.toDecimal192(),
                        totalCount = item.totalCount
                    ).takeIf { item.totalCount > 0 }
                }
                else -> null
            }
        }
    }
}
