package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.RadixConnectMobile
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val radixConnectMobile: RadixConnectMobile,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(deepLink: String) {
        val sessionRequest = runCatching { radixConnectMobile.handleDeepLink(deepLink) }.onFailure {
            Timber.d("Failed to parse deep link: $deepLink. Error: ${it.message}")
            return
        }.getOrThrow()

        incomingRequestRepository.addMobileConnectRequest(
            sessionRequest.interaction.toDomainModel(
                remoteEntityId = IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession(
                    id = sessionRequest.sessionId.toString(),
                    originVerificationUrl = if (sessionRequest.originRequiresValidation) sessionRequest.origin else null
                )
            ).getOrThrow()
        )
    }
}
