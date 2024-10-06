package com.babylon.wallet.android.domain.model.messages

sealed interface RemoteEntityID {

    val id: String

    data class RadixMobileConnectRemoteSession(
        override val id: String,
        val originVerificationUrl: String? = null
    ) : RemoteEntityID {

        val needOriginVerification: Boolean = originVerificationUrl != null
    }

    data class ConnectorId(override val id: String) : RemoteEntityID
}
