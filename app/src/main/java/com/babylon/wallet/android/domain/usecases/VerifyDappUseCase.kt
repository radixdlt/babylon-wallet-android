package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.DecodeAddressRequest
import kotlinx.coroutines.flow.first
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import javax.inject.Inject

class VerifyDappUseCase @Inject constructor(
    private val dappMetadataRepository: DappMetadataRepository,
    private val dAppMessenger: DappMessenger,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(request: IncomingRequest): Result<Boolean> {
        if (request.metadata.isInternal) {
            return Result.Success(true)
        }

        val developerMode = getProfileUseCase.security.first().isDeveloperModeEnabled
        val decodeResult = RadixEngineToolkit.decodeAddress(
            request = DecodeAddressRequest(request.metadata.dAppDefinitionAddress)
        )
        if (decodeResult.isFailure) {
            dAppMessenger.sendWalletInteractionResponseFailure(
                dappId = request.remoteClientId,
                requestId = request.id,
                error = WalletErrorType.InvalidRequest
            )
            return Result.Error(DappRequestException(DappRequestFailure.InvalidRequest))
        }
        return if (developerMode) {
            Result.Success(true)
        } else {
            val validationResult = dappMetadataRepository.verifyDapp(
                origin = request.metadata.origin,
                dAppDefinitionAddress = request.metadata.dAppDefinitionAddress
            )
            validationResult.onError { e ->
                (e as? DappRequestException)?.let {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        dappId = request.remoteClientId,
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
