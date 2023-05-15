package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import timber.log.Timber
import javax.inject.Inject

class VerifyRequestVersionCompatibilityUseCase @Inject constructor(
    private val dAppMessenger: DappMessenger
) {

    suspend operator fun invoke(request: IncomingRequest): Result<Unit> {
        val currentVersion = WalletInteraction.Metadata.VERSION
        val requestVersion = request.metadata.version
        return if (requestVersion != currentVersion) {
            Timber.e("The version of the request: $requestVersion is incompatible. Wallet version: $currentVersion")
            dAppMessenger.sendWalletInteractionResponseFailure(
                dappId = request.remoteClientId,
                requestId = request.id,
                error = WalletErrorType.IncompatibleVersion
            )
            Result.Error()
        } else {
            Result.Success(Unit)
        }
    }
}
