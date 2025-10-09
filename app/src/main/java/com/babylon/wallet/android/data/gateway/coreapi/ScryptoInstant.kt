package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScryptoInstant(
    @SerialName("unix_timestamp_seconds")
    val unixTimestampSeconds: String? = null,

    @Contextual @SerialName("date_time")
    val dateTime: String? = null
)