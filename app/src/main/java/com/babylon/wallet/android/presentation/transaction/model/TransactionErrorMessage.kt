package com.babylon.wallet.android.presentation.transaction.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.common.UiMessage
import rdx.works.profile.domain.ProfileException

data class TransactionErrorMessage(
    private val error: Throwable?
) {

    private val isNoMnemonicErrorVisible = error?.cause is ProfileException.NoMnemonic

    /**
     * True when this error will end up abandoning the transaction. Displayed as a dialog.
     */
    val isTerminalError: Boolean
        get() = isNoMnemonicErrorVisible ||
            error is RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits ||
            error is RadixWalletException.PrepareTransactionException.FailedToFindSigningEntities ||
            error is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction ||
            error is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent

    val uiMessage: UiMessage = UiMessage.ErrorMessage(error)

    @Composable
    fun getTitle(): String {
        return if (isNoMnemonicErrorVisible) {
            stringResource(id = R.string.transactionReview_noMnemonicError_title)
        } else if (error is RadixWalletException.LedgerCommunicationException.FailedToSignTransaction) {
            stringResource(id = R.string.ledgerHardwareDevices_couldNotSign_title)
        } else {
            stringResource(id = R.string.common_errorAlertTitle)
        }
    }
}