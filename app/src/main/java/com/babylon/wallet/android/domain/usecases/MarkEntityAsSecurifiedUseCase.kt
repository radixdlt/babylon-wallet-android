package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class MarkEntityAsSecurifiedUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(entityAddress: AddressOfAccountOrPersona) = runCatching {
        sargonOsManager.sargonOs.markEntitiesAsSecurified(entityAddresses = listOf(entityAddress))
    }
}
