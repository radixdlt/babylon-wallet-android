package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.os.SargonOsManager
import timber.log.Timber
import javax.inject.Inject

class CheckAccountsDeletedOnLedgerUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager
) {

    suspend fun sync() = runCatching {
        Timber.tag("Bakos").d("Checking accounts deleted")
        sargonOsManager.sargonOs.syncAccountsDeletedOnLedger()
    }.getOrDefault(false)

    suspend fun check(
        networkId: NetworkId,
        accountAddresses: List<AccountAddress>
    ) = runCatching {
        sargonOsManager.sargonOs.checkAccountsDeletedOnLedger(
            networkId = networkId,
            accountAddresses = accountAddresses
        )
    }

}