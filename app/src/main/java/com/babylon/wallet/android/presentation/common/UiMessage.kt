package com.babylon.wallet.android.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import rdx.works.core.UUIDGenerator

sealed class UiMessage(val id: String = UUIDGenerator.uuid().toString()) {
    sealed class InfoMessage : UiMessage() {
        object InvalidMnemonic : InfoMessage()
        object InvalidPayload : InfoMessage()
        object NoMnemonicForAccounts : InfoMessage()
        object NoAccountsForLedger : InfoMessage()
        data class LedgerAlreadyExist(val label: String) : InfoMessage()

        @Composable
        fun userFriendlyDescriptionRes(): String {
            return when (this) {
                InvalidMnemonic -> stringResource(R.string.importOlympiaAccounts_invalidMnemonic)
                InvalidPayload -> stringResource(R.string.importOlympiaAccounts_invalidPayload)
                NoMnemonicForAccounts -> stringResource(R.string.importOlympiaAccounts_noMnemonicFound)
                NoAccountsForLedger -> stringResource(R.string.common_somethingWentWrong)
                is LedgerAlreadyExist -> stringResource(id = R.string.addLedger_alreadyAddedAlert_message, label)
            }
        }
    }

    data class ErrorMessage(val error: Throwable? = null) : UiMessage()

    @Composable
    fun getUserFriendlyDescription(): String? {
        return when (this) {
            is ErrorMessage -> {
                ((error as? DappRequestException)?.failure?.toDescriptionRes() ?: (error as? DappRequestFailure)?.toDescriptionRes())?.let {
                    stringResource(id = it)
                }
            }

            is InfoMessage -> this.userFriendlyDescriptionRes()
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
