package com.babylon.wallet.android.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.UUIDGenerator

@Serializable
sealed class UiMessage(val id: String = UUIDGenerator.uuid().toString()) {

    abstract fun getMessage(context: Context): String

    @Serializable
    @SerialName("info_message")
    sealed class InfoMessage : UiMessage() {
        @Serializable
        @SerialName("invalid_mnemonic")
        object InvalidMnemonic : InfoMessage()

        @Serializable
        @SerialName("invalid_payload")
        object InvalidPayload : InfoMessage()

        @Serializable
        @SerialName("invalid_no_mnemonic_for_accounts")
        object NoMnemonicForAccounts : InfoMessage()

        @Serializable
        @SerialName("invalid_no_accounts_for_ledger")
        object NoAccountsForLedger : InfoMessage()

        @Serializable
        @SerialName("invalid_ledger_already_exist")
        data class LedgerAlreadyExist(val label: String) : InfoMessage()

        @Serializable
        @SerialName("wallet_exported")
        object WalletExported : InfoMessage()

        override fun getMessage(context: Context): String = when (this) {
            InvalidMnemonic -> context.getString(R.string.importOlympiaAccounts_invalidMnemonic)
            InvalidPayload -> context.getString(R.string.importOlympiaAccounts_invalidPayload)
            NoMnemonicForAccounts -> context.getString(R.string.importOlympiaAccounts_noMnemonicFound)
            NoAccountsForLedger ->
                "No addresses verified. The currently connected Ledger device is not related " +
                    "to any accounts to be imported, or has already been used."
            is LedgerAlreadyExist -> context.getString(R.string.addLedgerDevice_alreadyAddedAlert_message, label)
            WalletExported -> context.getString(R.string.profileBackup_manualBackups_successMessage)
        }
    }

    @Serializable
    @SerialName("error_message")
    data class ErrorMessage(
        @StringRes
        private val userFriendlyDescription: Int?,
        private val nonUserFriendlyDescription: String?
    ) : UiMessage() {

        override fun getMessage(context: Context): String = userFriendlyDescription?.let {
            context.getString(it)
        } ?: nonUserFriendlyDescription.orEmpty()

        companion object {
            fun from(error: Throwable?) = ErrorMessage(
                userFriendlyDescription = (error as? DappRequestException)?.failure?.toDescriptionRes()
                    ?: (error as? DappRequestFailure)?.toDescriptionRes(),
                nonUserFriendlyDescription = error?.message
            )
        }
    }
}

@Composable
fun UiMessage.getMessage(): String = getMessage(context = LocalContext.current)
