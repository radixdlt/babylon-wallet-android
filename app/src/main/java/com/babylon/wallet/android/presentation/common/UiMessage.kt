package com.babylon.wallet.android.presentation.common

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.DappRequestException
import rdx.works.core.UUIDGenerator

sealed class UiMessage(val id: String = UUIDGenerator.uuid().toString()) {
    data class InfoMessage(val type: InfoMessageType? = null) : UiMessage()
    data class ErrorMessage(val error: Throwable? = null) : UiMessage()

    @StringRes
    fun getUserFriendlyDescriptionRes(): Int? {
        return when (this) {
            is ErrorMessage -> (this.error as? DappRequestException)?.failure?.toDescriptionRes()
            is InfoMessage -> this.type?.userFriendlyDescriptionRes()
        }
    }

    fun getErrorMessage(): String? {
        return if (this is ErrorMessage) {
            error?.message
        } else {
            null
        }
    }
}

enum class InfoMessageType {
    GatewayUpdated, GatewayInvalid, InvalidMnemonic, InvalidPayload;

    @StringRes
    fun userFriendlyDescriptionRes(): Int {
        return when (this) {
            GatewayUpdated -> R.string.gateway_updated
            GatewayInvalid -> R.string.gateway_invalid
            InvalidMnemonic -> R.string.invalid_mnemonic
            InvalidPayload -> R.string.invalid_payload
        }
    }
}
