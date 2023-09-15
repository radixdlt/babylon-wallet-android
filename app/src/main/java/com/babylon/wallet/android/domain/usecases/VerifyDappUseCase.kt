package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.radixdlt.ret.Address
import kotlinx.coroutines.flow.first
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import javax.inject.Inject

class VerifyDappUseCase @Inject constructor(
    private val dAppRepository: DAppRepository,
    private val dAppMessenger: DappMessenger,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(request: IncomingRequest): Result<Boolean> {
        val developerMode = getProfileUseCase.security.first().isDeveloperModeEnabled
        val decodeResult = runCatching { Address(request.metadata.dAppDefinitionAddress) }
        if (decodeResult.isFailure) {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = request.remoteConnectorId,
                requestId = request.id,
                error = WalletErrorType.InvalidRequest
            )
            return Result.failure(DappRequestException(DappRequestFailure.InvalidRequest))
        }
        return if (developerMode) {
            Result.success(true)
        } else {
            val validationResult = dAppRepository.verifyDapp(
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            )
            validationResult.onFailure { e ->
                (e as? DappRequestException)?.let {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteConnectorId,
                        requestId = request.id,
                        error = it.failure.toWalletErrorType(),
                        message = it.failure.getDappMessage()
                    )
                }
            }
            validationResult
        }
    }
}
