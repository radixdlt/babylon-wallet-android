package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class TombstoneAccountsUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(accountAddresses: Set<AccountAddress>) {
        // TODO invoke sargon
    }

}