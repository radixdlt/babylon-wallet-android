package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class CommitProvisionalShieldUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(address: AddressOfAccountOrPersona): Result<Unit> =
        sargonOsManager.callSafely(dispatcher) {
            commitProvisionalSecurityState(address)
        }.onFailure {
            Timber.e(it)
        }.onSuccess {
            Timber.i("Provisional security state commited.")
        }
}
