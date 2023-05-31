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
    InvalidMnemonic, InvalidPayload, NoMnemonicForAccounts, NoAccountsForLedger;

    @StringRes
    fun userFriendlyDescriptionRes(): Int {
        return when (this) {
            InvalidMnemonic -> R.string.importOlympiaAccounts_invalidMnemonic
            InvalidPayload -> R.string.importOlympiaAccounts_invalidPayload
            NoMnemonicForAccounts -> R.string.importOlympiaAccounts_noMnemonicFound
            NoAccountsForLedger -> R.string.diverging_noHardwareAccounts
        }
    }
}
