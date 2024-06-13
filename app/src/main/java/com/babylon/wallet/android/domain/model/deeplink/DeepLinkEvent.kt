package com.babylon.wallet.android.domain.model.deeplink

import com.radixdlt.sargon.RadixConnectMobileLinkRequest

sealed interface DeepLinkEvent {

    data class MobileConnectLinkRequest(
        val link: RadixConnectMobileLinkRequest
    ) : DeepLinkEvent
}