package com.babylon.wallet.android.presentation.transaction.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.common.UiMessage
import rdx.works.profile.domain.ProfileException

data class TransactionErrorMessage(
    val error: Throwable?
) {

    /**
     * True when this error will end up abandoning the transaction. Displayed as a dialog.
     */
    val isTerminalError: Boolean
        get() =
            error is RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits ||
            error is RadixWalletException.PrepareTransactionException.RequestNotFound ||
            error is RadixWalletException.DappRequestException.PreviewError ||
            error is RadixWalletException.DappRequestException.InvalidPreAuthorizationExpirationTooClose ||
            error is RadixWalletException.DappRequestException.InvalidPreAuthorizationExpired ||
            error is RadixWalletException.DappRequestException.UnacceptableManifest

    val uiMessage: UiMessage = UiMessage.ErrorMessage(error)

    @Composable
    fun getTitle(): String = stringResource(id = R.string.common_errorAlertTitle)
}
