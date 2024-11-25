package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.PreAuthorizationStatusData
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.PreAuthorizationStatus
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PollPreAuthorizationStatusUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        intentHash: SubintentHash,
        requestId: String,
        expiration: DappToWalletInteractionSubintentExpiration
    ): PreAuthorizationStatusData {
        return withContext(dispatcher) {
            val sargonOs = sargonOsManager.sargonOs
            val txId = intentHash.bech32EncodedTxId
            val status = sargonOs.pollPreAuthorizationStatus(
                intentHash = intentHash,
                expiration = expiration
            )

            when (status) {
                PreAuthorizationStatus.Expired -> PreAuthorizationStatusData(
                    txId = txId,
                    requestId = requestId,
                    result = PreAuthorizationStatusData.Status.Expired
                )
                is PreAuthorizationStatus.Success -> PreAuthorizationStatusData(
                    txId = txId,
                    requestId = requestId,
                    result = PreAuthorizationStatusData.Status.Success(status.intentHash)
                )
            }
        }
    }
}
