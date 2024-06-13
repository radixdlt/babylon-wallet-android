package com.babylon.wallet.android.domain.usecases.deeplink

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.data.repository.RcrRepository
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.deeplink.DeepLinkEvent
import com.radixdlt.sargon.RadixConnectMobileConnectRequest
import com.radixdlt.sargon.newMobileConnectRequest
import timber.log.Timber
import javax.inject.Inject

class ProcessDeepLinkUseCase @Inject constructor(
    private val rcrRepository: RcrRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend operator fun invoke(deepLink: String): DeepLinkEvent? {
        val mobileConnectRequest = runCatching { newMobileConnectRequest(deepLink) }.onFailure {
            Timber.d("Failed to parse deep link: $deepLink. Error: ${it.message}")
            return null
        }.getOrThrow()

        return when (mobileConnectRequest) {
            is RadixConnectMobileConnectRequest.DappInteraction -> {
                val sessionId = mobileConnectRequest.v1.sessionId.toString()

                rcrRepository.getRequest(
                    sessionId = sessionId,
                    interactionId = mobileConnectRequest.v1.interactionId.toString()
                ).mapCatching { walletInteraction ->
                    walletInteraction.toDomainModel(
                        remoteEntityId = IncomingMessage.RemoteEntityID.RadixMobileConnectRemoteSession(sessionId)
                    )
                }.onSuccess { request ->
                    incomingRequestRepository.add(request)
                }.onFailure {
                    Timber.d(it)
                }

                null
            }
            is RadixConnectMobileConnectRequest.Link -> {
                DeepLinkEvent.MobileConnectLinkRequest(
                    link = mobileConnectRequest.v1
                )
            }
        }
    }
}