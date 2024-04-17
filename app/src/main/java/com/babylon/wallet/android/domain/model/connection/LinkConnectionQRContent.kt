package com.babylon.wallet.android.domain.model.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkConnectionQRContent(
    @SerialName("password")
    val password: String,
    @SerialName("publicKey")
    val publicKey: String,
    @SerialName("purpose")
    val purpose: String,
    @SerialName("signature")
    val signature: String
)
