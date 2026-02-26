package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(deepLink: String): Result<DeepLinkProcessingResult> {
        return runCatching {
            val profileFinishedOnboarding = getProfileUseCase.finishedOnboardingProfile() != null
            val sessionRequest = sargonOsManager.sargonOs.radixConnectMobile().handleDeepLink(deepLink)
            val request = sessionRequest.interaction.toDomainModel(
                remoteEntityId = RemoteEntityID.RadixMobileConnectRemoteSession(
                    id = sessionRequest.sessionId.toString(),
                    originVerificationUrl = if (sessionRequest.originRequiresValidation) sessionRequest.origin else null
                )
            ).getOrThrow()
            if (!profileFinishedOnboarding) {
                incomingRequestRepository.setBufferedRequest(request)
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
    data class Processed(val request: DappToWalletInteraction) : DeepLinkProcessingResult
}
