package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CommitProvisionalShieldUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(address: AddressOfAccountOrPersona) = sargonOsManager.callSafely(dispatcher) {
        commitProvisionalSecurityState(address)
    }
}
