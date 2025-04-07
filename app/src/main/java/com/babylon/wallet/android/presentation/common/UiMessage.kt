package com.babylon.wallet.android.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.toMessage
import com.babylon.wallet.android.domain.toUserFriendlyMessage
import com.radixdlt.sargon.CommonException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.UUIDGenerator
import rdx.works.profile.domain.ProfileException

@Serializable
sealed class UiMessage(val id: String = UUIDGenerator.uuid().toString()) {

    @Composable
    abstract fun getMessage(): String

    @Serializable
    @SerialName("info_message")
    sealed class InfoMessage : UiMessage() {

        @Serializable
        @SerialName("invalid_payload")
        data object InvalidPayload : InfoMessage()

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

        @Serializable
        @SerialName("spot_check_outcome")
        data class SpotCheckOutcome(
            @SerialName("is_success")
            val isSuccess: Boolean
        ) : InfoMessage()

        @Serializable
        @SerialName("rename_successful")
        data object RenameSuccessful : InfoMessage()

        @Composable
        override fun getMessage(): String = when (this) {
            InvalidPayload -> stringResource(id = R.string.importOlympiaAccounts_invalidPayload)
            NoAccountsForLedger ->
                "No addresses verified. The currently connected Ledger device is not related to any " +
                    "accounts to be imported, or has already been used." // TODO string.importOlympiaAccounts_noAddresses)
            is LedgerAlreadyExist -> stringResource(id = R.string.addLedgerDevice_alreadyAddedAlert_message, label)
            WalletExported -> stringResource(id = R.string.profileBackup_manualBackups_successMessage)
            NpsSurveySubmitted -> "Thank you!"
            is SpotCheckOutcome -> stringResource(
                id = if (isSuccess) {
                    R.string.factorSources_detail_spotCheckSuccess
                } else {
                    R.string.factorSources_detail_spotCheckFailure
                }
            )
            RenameSuccessful -> stringResource(R.string.renameLabel_success)
        }
    }

    data class ErrorMessage(
        private val error: Throwable?
    ) : UiMessage() {

        @Composable
        override fun getMessage(): String {
            val message = when (error) {
                is ProfileException -> return error.toUserFriendlyMessage()
                is CommonException -> return error.toMessage()
                else -> error?.asRadixWalletException()?.toUserFriendlyMessage(LocalContext.current) ?: error?.message
            }
            return if (message.isNullOrEmpty()) {
                stringResource(id = R.string.common_somethingWentWrong)
            } else {
                message
            }
        }
    }

    @Composable
    fun ProfileException.toUserFriendlyMessage(): String {
        return when (this) {
            is ProfileException.InvalidSnapshot -> stringResource(id = R.string.recoverProfileBackup_incompatibleWalletDataLabel)
            is ProfileException.InvalidPassword -> stringResource(id = R.string.recoverProfileBackup_passwordWrong)
            is ProfileException.NoMnemonic -> "Please restore your Seed Phrase and try again"
            is ProfileException.SecureStorageAccess -> "There was issue tying to access mnemonic secure storage"
            is ProfileException.AuthenticationSigningAlreadyExist -> "Signing Entity $entity already has authenticationSigning"
            ProfileException.InvalidMnemonic -> stringResource(id = R.string.importOlympiaAccounts_invalidMnemonic)
        }
    }
}
