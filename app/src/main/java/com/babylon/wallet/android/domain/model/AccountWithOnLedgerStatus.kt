package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.Account

data class AccountWithOnLedgerStatus(
    val account: Account,
    val status: Status = Status.Inactive
) {
    /**
     * Active: account had at least one transaction
     */
    enum class Status {
        Active, Inactive
    }
}
