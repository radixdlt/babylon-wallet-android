package com.babylon.wallet.android.domain.model.locker

import com.radixdlt.sargon.LockerAddress

data class AccountLockerDeposit(
    val lockerAddress: LockerAddress,
    val dAppName: String
)
