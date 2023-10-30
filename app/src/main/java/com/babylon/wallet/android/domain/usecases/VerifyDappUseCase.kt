package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.babylon.wallet.android.domain.toConnectorExtensionError
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
            return Result.failure(RadixWalletException.DappRequestException.InvalidRequest)
        }
        return if (developerMode) {
            Result.success(true)
        } else {
            val validationResult = dAppRepository.verifyDapp(
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            )
            validationResult.onFailure { throwable ->
                throwable.asRadixWalletException()?.let { radixWalletException ->
                    val walletErrorType = radixWalletException.toConnectorExtensionError() ?: return@let
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        remoteConnectorId = request.remoteConnectorId,
                        requestId = request.id,
                        error = walletErrorType,
                        message = radixWalletException.getDappMessage()
                    )
                }
            }
            validationResult
        }
    }
}
