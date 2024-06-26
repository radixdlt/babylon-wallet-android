package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.BufferedMobileConnectRequestRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.RadixConnectMobile
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val radixConnectMobile: RadixConnectMobile,
    private val getProfileUseCase: GetProfileUseCase,
    private val bufferedMobileConnectRequestRepository: BufferedMobileConnectRequestRepository
) {

    suspend operator fun invoke(deepLink: String): Result<DeepLinkProcessingResult> {
        return runCatching {
            val profileInitialized = getProfileUseCase.isInitialized()
            val sessionRequest = radixConnectMobile.handleDeepLink(deepLink)
            val request = sessionRequest.interaction.toDomainModel(
                remoteEntityId = IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession(
                    id = sessionRequest.sessionId.toString(),
                    originVerificationUrl = if (sessionRequest.originRequiresValidation) sessionRequest.origin else null
                )
            ).getOrThrow()
            if (!profileInitialized) {
                bufferedMobileConnectRequestRepository.setBufferedRequest(request)
                return Result.success(DeepLinkProcessingResult.Buffered)
            }
            DeepLinkProcessingResult.Processed(request)
        }.onFailure {
            Timber.d("Failed to parse deep link: $deepLink. Error: ${it.message}")
        }
    }
}

sealed interface DeepLinkProcessingResult {
    data object Buffered : DeepLinkProcessingResult
    data class Processed(val request: IncomingMessage.IncomingRequest) : DeepLinkProcessingResult
}
