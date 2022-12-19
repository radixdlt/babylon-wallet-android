package com.babylon.wallet.android.presentation.common

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import java.util.UUID

data class UiMessage(
    val messageType: InfoMessageType? = null,
    val error: Throwable? = null,
    val id: String = UUID.randomUUID().toString()
)

enum class InfoMessageType {
    GatewayUpdated, GatewayInvalid;

    @StringRes
    fun userFriendlyDescriptionRes(): Int {
        return when (this) {
            GatewayUpdated -> R.string.gateway_updated
            GatewayInvalid -> R.string.gateway_invalid
        }
    }
}
