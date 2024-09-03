package com.babylon.wallet.android.data.repository.cache.database.locker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress

@Dao
interface AccountLockerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTouchedAt(accountLockerState: List<AccountLockerTouchedAtEntity>)

    @Query(
        """
        SELECT * FROM AccountLockerTouchedAtEntity
        WHERE account_address = :accountAddress AND locker_address in (:lockerAddresses)
    """
    )
    fun getTouchedAt(
        accountAddress: AccountAddress,
        lockerAddresses: Set<LockerAddress>
    ): List<AccountLockerTouchedAtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertVaultItems(items: List<AccountLockerVaultItemEntity>)

    @Query(
        """
        SELECT * FROM AccountLockerVaultItemEntity
        WHERE account_address = :accountAddress AND locker_address = :lockerAddress
    """
    )
    fun getVaultItems(
        accountAddress: AccountAddress,
        lockerAddress: LockerAddress
    ): List<AccountLockerVaultItemEntity>

    @Query(
        """
        DELETE FROM AccountLockerVaultItemEntity
        WHERE account_address = :accountAddress AND locker_address = :lockerAddress
    """
    )
    fun deleteVaultItems(
        accountAddress: AccountAddress,
        lockerAddress: LockerAddress
    )
}
