package com.babylon.wallet.android.data.repository.cache.database.locker

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.gateway.generated.models.StateAccountLockersTouchedAtResponse
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.extensions.SargonException
import com.radixdlt.sargon.extensions.init

@Entity(primaryKeys = ["locker_address", "account_address"])
data class AccountLockerTouchedAtEntity(
    @ColumnInfo(name = "locker_address")
    val lockerAddress: LockerAddress,
    @ColumnInfo(name = "account_address")
    val accountAddress: AccountAddress,
    @ColumnInfo(name = "last_touched_at_state_version")
    val lastTouchedAtStateVersion: Long,
    @ColumnInfo(name = "at_ledger_state")
    val atLedgerState: Long
) {

    fun isSame(other: AccountLockerTouchedAtEntity): Boolean {
        return lockerAddress == other.lockerAddress && accountAddress == other.accountAddress
    }

    companion object {

        @Throws(SargonException::class)
        fun from(response: StateAccountLockersTouchedAtResponse): List<AccountLockerTouchedAtEntity> {
            return response.items.map {
                AccountLockerTouchedAtEntity(
                    lockerAddress = LockerAddress.init(it.lockerAddress),
                    accountAddress = AccountAddress.init(it.accountAddress),
                    lastTouchedAtStateVersion = it.lastTouchedAtStateVersion,
                    atLedgerState = response.ledgerState.stateVersion
                )
            }
        }
    }
}
