package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class UpdateProvisionalShieldUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun commit(address: AddressOfAccountOrPersona): Result<Unit> =
        sargonOsManager.callSafely(dispatcher) {
            commitProvisionalSecurityState(address)
        }.onFailure {
            Timber.e(it)
        }.onSuccess {
            Timber.i("Provisional security state commited.")
        }

    suspend fun remove(address: AddressOfAccountOrPersona): Result<Unit> =
        sargonOsManager.callSafely(dispatcher) {
            removeProvisionalSecurityState(address)
        }.onFailure {
            Timber.e(it)
        }.onSuccess {
            Timber.i("Provisional security state commited.")
        }
}
