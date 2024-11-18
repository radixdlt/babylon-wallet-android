package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class TombstoneAccountUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
) {

    suspend operator fun invoke(accountAddress: AccountAddress) = runCatching {
        sargonOsManager.sargonOs.markAccountAsTombstoned(accountAddress)
    }
}