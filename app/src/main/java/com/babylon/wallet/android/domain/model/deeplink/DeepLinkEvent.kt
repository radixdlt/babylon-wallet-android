package com.babylon.wallet.android.domain.model.deeplink

import com.radixdlt.sargon.RadixConnectMobileSessionRequest

sealed interface DeepLinkEvent {

    data class MobileConnectVerifyRequest(
        val request: RadixConnectMobileSessionRequest
    ) : DeepLinkEvent
}
