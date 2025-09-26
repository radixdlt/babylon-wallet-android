@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.dialogs.transaction

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.transaction.TransactionStatusDialogViewModel.State.DismissInfo.REQUIRE_COMPLETION
import com.babylon.wallet.android.presentation.dialogs.transaction.TransactionStatusDialogViewModel.State.DismissInfo.STOP_WAITING
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.FailureDialogContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.radixdlt.sargon.DappWalletInteractionErrorType

@Composable
fun TransactionStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: TransactionStatusDialogViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetState ->
            state.isDismissible
        }
    )

    LaunchedEffect(state.statusEnum) {
        sheetState.show()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionStatusDialogViewModel.Event.DismissDialog -> {
                    sheetState.hide()
                }
            }
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            onClose()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        showDragHandle = state.isDismissible,
        onDismissRequest = viewModel::onDismiss,
        wrapContent = true,
        windowInsets = {
            WindowInsets.navigationBars
        },
        sheetContent = {
            Column {
                RadixCenteredTopAppBar(
                    title = "",
                    backIconType = BackIconType.Close,
                    onBackClick = viewModel::onDismiss
                )
                Box {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isCompleting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CompletingContent(transactionId = state.transactionId)
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isFailed,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val title = when (state.walletErrorType) {
                            DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_FAILED_TRANSACTION_STATUS -> {
                                stringResource(id = R.string.transactionStatus_failed_title)
                            }

                            DappWalletInteractionErrorType.SUBMITTED_TRANSACTION_HAS_REJECTED_TRANSACTION_STATUS -> {
                                stringResource(id = R.string.transactionStatus_rejected_title)
                            }

//                        DappWalletInteractionErrorType.SubmittedTransactionHasTemporarilyRejectedTransactionStatus -> {
//                            stringResource(id = R.string.transactionStatus_error_title)
//                        } // TODO verify if we want to add that error to sargon

                            else -> {
                                stringResource(id = R.string.common_somethingWentWrong)
                            }
                        }
                        FailureDialogContent(
                            title = title,
                            subtitle = state.failureError,
                            transactionId = state.transactionId,
                            isMobileConnect = state.status.isMobileConnect
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isSuccess,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        // Need to send the correct transaction id
                        SuccessContent(
                            transactionId = state.transactionId,
                            isMobileConnect = state.status.isMobileConnect,
                            dAppName = state.status.dAppName,
                            isInternal = state.status.isInternal
                        )
                    }

                    state.dismissInfo?.let {
                        InfoDialog(
                            type = it,
                            onClose = viewModel::onInfoClose
                        )
                    }
                }
            }
        },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = state.isDismissible
        )
    )
}

@Composable
private fun InfoDialog(
    type: TransactionStatusDialogViewModel.State.DismissInfo,
    onClose: (Boolean) -> Unit
) {
    BasicPromptAlertDialog(
        finish = onClose,
        message = {
            Text(
                text = stringResource(
                    id = when (type) {
                        STOP_WAITING -> R.string.transactionStatus_dismissDialog_message
                        REQUIRE_COMPLETION -> R.string.transactionStatus_dismissalDisabledDialog_text
                    }
                ),
                color = RadixTheme.colors.text
            )
        },
        confirmText = stringResource(id = R.string.common_ok),
        dismissText = null,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
