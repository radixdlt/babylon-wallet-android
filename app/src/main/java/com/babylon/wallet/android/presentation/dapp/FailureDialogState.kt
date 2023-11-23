package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.RadixWalletException

sealed interface FailureDialogState {
    data object Closed : FailureDialogState
    data class Open(val dappRequestException: RadixWalletException.DappRequestException) : FailureDialogState
}
