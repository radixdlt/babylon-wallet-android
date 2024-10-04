package com.babylon.wallet.android.domain.model.messages

sealed interface RemoteEntityID {

    val value: String

    data class RadixMobileConnectRemoteSession(val id: String, val originVerificationUrl: String? = null) :
        RemoteEntityID {
        override val value: String
            get() = id

        val needOriginVerification: Boolean = originVerificationUrl != null
    }

    data class ConnectorId(val id: String) : RemoteEntityID {
        override val value: String
            get() = id
    }
}
