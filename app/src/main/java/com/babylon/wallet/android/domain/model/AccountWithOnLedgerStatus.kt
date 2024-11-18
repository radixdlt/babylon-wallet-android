package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.Account

data class AccountWithOnLedgerStatus(
    val account: Account,
    val status: Status
) {
    /**
     * Active: account had at least one transaction,
     *
     */
    enum class Status {
        /**
         * An account is active if it had at least one transaction and is not deleted
         */
        Active,

        /**
         * An account is inactive if it had no transactions and was not deleted
         */
        Inactive,

        /**
         * The user has deleted this account by swallowing its badge.
         */
        Deleted
    }
}
