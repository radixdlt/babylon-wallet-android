package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.BufferedDeepLinkRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.RadixConnectMobile
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val radixConnectMobile: RadixConnectMobile,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val bufferedDeepLinkRepository: BufferedDeepLinkRepository
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
            if (profileInitialized) {
                incomingRequestRepository.addMobileConnectRequest(request)
                DeepLinkProcessingResult.PROCESSED
            } else {
                bufferedDeepLinkRepository.setBufferedRequest(request)
                DeepLinkProcessingResult.BUFFERED
            }
        }.onFailure {
            Timber.d("Failed to parse deep link: $deepLink. Error: ${it.message}")
        }
    }
}

enum class DeepLinkProcessingResult {
    PROCESSED,
    BUFFERED
}
