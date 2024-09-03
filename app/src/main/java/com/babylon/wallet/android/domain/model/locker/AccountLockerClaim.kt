package com.babylon.wallet.android.domain.model.locker

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountLockerClaimableResource

data class AccountLockerClaim(
    val accountAddress: AccountAddress,
    val lockerAddress: String,
    val claimableResources: List<AccountLockerClaimableResource>
)
