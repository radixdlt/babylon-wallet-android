package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.deeplink.DeepLinkEvent
import com.radixdlt.sargon.RadixConnectMobile
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val radixConnectMobile: RadixConnectMobile,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(deepLink: String): DeepLinkEvent? {
        val sessionRequest = runCatching { radixConnectMobile.handleDeepLink(deepLink) }.onFailure {
            Timber.d("Failed to parse deep link: $deepLink. Error: ${it.message}")
            return null
        }.getOrThrow()

        return if (sessionRequest.originRequiresValidation) {
            DeepLinkEvent.MobileConnectVerifyRequest(
                request = sessionRequest
            )
        } else {
            incomingRequestRepository.add(
                sessionRequest.interaction.toDomainModel(
                    remoteEntityId = IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession(sessionRequest.sessionId.toString())
                ).getOrThrow()
            )
            null
        }
    }
}
