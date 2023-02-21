package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class VerifyDappUseCase @Inject constructor(
    private val dappMetadataRepository: DappMetadataRepository,
    private val dAppMessenger: DAppMessenger,
    private val preferencesManager: PreferencesManager
) {

    @Suppress("LongMethod")
    suspend operator fun invoke(request: IncomingRequest) =
        coroutineScope {
            val developerMode = preferencesManager.isInDeveloperMode()
            if (developerMode) {
                Result.Success(true)
            } else {
                val validationResult = dappMetadataRepository.verifyDapp(
                    request.metadata.origin,
                    request.metadata.dAppDefinitionAddress
                )
                validationResult.onError { e ->
                    (e as? TransactionApprovalException)?.let {
                        dAppMessenger.sendWalletInteractionResponseFailure(
                            request.id,
                            it.failure.toWalletErrorType(),
                            it.failure.getDappMessage()
                        )
                    }
                }
                validationResult
            }
        }
}
