package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.PreAuthorizationStatusData
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.PreAuthorizationStatus
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.mapError
import javax.inject.Inject

class GetPreAuthorizationStatusUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        intentHash: SubintentHash,
        requestId: String,
        expiration: Timestamp
    ): PreAuthorizationStatusData = withContext(dispatcher) {
        val txId = intentHash.bech32EncodedTxId

        runCatching {
            val sargonOs = sargonOsManager.sargonOs
            val status = sargonOs.pollPreAuthorizationStatus(
                intentHash = intentHash,
                expirationTimestamp = expiration
            )

            PreAuthorizationStatusData(
                txId = txId,
                requestId = requestId,
                result = when (status) {
                    PreAuthorizationStatus.Expired -> PreAuthorizationStatusData.Status.Expired
                    is PreAuthorizationStatus.Success -> PreAuthorizationStatusData.Status.Success(status.intentHash)
                }
            )
        }.mapError { RadixWalletException.TransactionSubmitException.FailedToPollTXStatus(txId) }.getOrThrow()
    }
}
