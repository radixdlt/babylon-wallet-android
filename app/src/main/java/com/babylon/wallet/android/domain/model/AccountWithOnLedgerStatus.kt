package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network

data class AccountWithOnLedgerStatus(
    val account: Network.Account,
    val status: Status = Status.Inactive
) {
    enum class Status {
        Active, Inactive
    }
}
