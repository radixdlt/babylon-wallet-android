package com.babylon.wallet.android.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.toUserFriendlyMessage
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.UUIDGenerator
import java.io.IOException

@Serializable
sealed class UiMessage(val id: String = UUIDGenerator.uuid().toString()) {

    @Composable
    abstract fun getMessage(): String

    @Serializable
    @SerialName("info_message")
    sealed class InfoMessage : UiMessage() {
        @Serializable
        @SerialName("invalid_mnemonic")
        data object InvalidMnemonic : InfoMessage()

        @Serializable
        @SerialName("invalid_snapshot")
        data object InvalidSnapshot : InfoMessage()

        @Serializable
        @SerialName("invalid_password")
        data object InvalidPassword : InfoMessage()

        @Serializable
        @SerialName("invalid_payload")
        data object InvalidPayload : InfoMessage()

        @Serializable
        @SerialName("invalid_no_mnemonic_for_accounts")
        data object NoMnemonicForAccounts : InfoMessage()

        @Serializable
        @SerialName("invalid_no_accounts_for_ledger")
        data object NoAccountsForLedger : InfoMessage()

        @Serializable
        @SerialName("invalid_ledger_already_exist")
        data class LedgerAlreadyExist(val label: String) : InfoMessage()

        @Serializable
        @SerialName("wallet_exported")
        data object WalletExported : InfoMessage()

        @Serializable
        @SerialName("nps_survey_submitted")
        data object NpsSurveySubmitted : InfoMessage()

        @Composable
        override fun getMessage(): String = when (this) {
            InvalidMnemonic -> stringResource(id = R.string.importOlympiaAccounts_invalidMnemonic)
            InvalidPayload -> stringResource(id = R.string.importOlympiaAccounts_invalidPayload)
            NoMnemonicForAccounts -> stringResource(id = R.string.importOlympiaAccounts_noMnemonicFound)
            NoAccountsForLedger ->
                "No addresses verified. The currently connected Ledger device is not related to any " +
                    "accounts to be imported, or has already been used." // TODO string.importOlympiaAccounts_noAddresses)
            is LedgerAlreadyExist -> stringResource(id = R.string.addLedgerDevice_alreadyAddedAlert_message, label)
            WalletExported -> stringResource(id = R.string.profileBackup_manualBackups_successMessage)
            InvalidSnapshot -> stringResource(id = R.string.recoverProfileBackup_incompatibleWalletDataLabel)
            InvalidPassword -> stringResource(id = R.string.recoverProfileBackup_passwordWrong)
            NpsSurveySubmitted -> "Thank you!"
        }
    }

    data class ErrorMessage(
        private val error: Throwable?
    ) : UiMessage() {

        @Composable
        override fun getMessage(): String {
            val message = error?.asRadixWalletException()?.toUserFriendlyMessage(LocalContext.current) ?: error?.message
            return if (message.isNullOrEmpty()) {
                stringResource(id = R.string.common_somethingWentWrong)
            } else {
                message
            }
        }
    }

    data class GoogleAuthErrorMessage(
        private val error: Throwable
    ) : UiMessage() {

        @Composable
        override fun getMessage(): String {
            return when (error) {
                is GoogleJsonResponseException -> stringResource(id = R.string.common_somethingWentWrong)
                is GooglePlayServicesAvailabilityException -> stringResource(id = R.string.common_somethingWentWrong)
                is UserRecoverableAuthException -> stringResource(id = R.string.common_somethingWentWrong)
                is UserRecoverableAuthIOException -> stringResource(id = R.string.common_somethingWentWrong)
                is GoogleAuthException -> stringResource(id = R.string.common_somethingWentWrong)
                is IOException -> stringResource(id = R.string.common_somethingWentWrong) + ", ${error.message}"
                else -> stringResource(id = R.string.common_somethingWentWrong)
            }
        }
    }
}
