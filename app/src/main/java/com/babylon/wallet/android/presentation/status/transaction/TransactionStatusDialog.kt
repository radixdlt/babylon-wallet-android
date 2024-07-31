package com.babylon.wallet.android.presentation.status.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.FailureDialogContent
import com.babylon.wallet.android.presentation.ui.composables.TransactionId
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
@Suppress("CyclomaticComplexMethod")
fun TransactionStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: TransactionStatusDialogViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dismissHandler = {
        viewModel.onDismiss()
    }
    BackHandler {
        dismissHandler()
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionStatusDialogViewModel.Event.DismissDialog -> onClose()
            }
        }
    }
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = dismissHandler,
        dragToDismissEnabled = !state.blockUntilComplete,
        showDefaultTopBar = !state.blockUntilComplete,
        showDragHandle = !state.blockUntilComplete,
        content = {
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

                if (state.isIgnoreTransactionModalShowing) {
                    BasicPromptAlertDialog(
                        finish = { accepted ->
                            if (accepted) {
                                viewModel.onDismissConfirmed()
                            } else {
                                viewModel.onDismissCanceled()
                            }
                        },
                        message = {
                            Text(text = stringResource(id = R.string.transactionStatus_dismissDialog_message))
                        },
                        confirmText = stringResource(id = R.string.common_ok),
                        dismissText = null
                    )
                }
            }
        }
    )
}

@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    transactionId: IntentHash?,
    isMobileConnect: Boolean
) {
    Column {
        Column(
            modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.defaultBackground)
                .padding(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Image(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
                ),
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.transactionStatus_success_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.transactionStatus_success_text),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            if (transactionId != null) {
                TransactionId(transactionId = transactionId)
            }
        }
        if (isMobileConnect) {
            HorizontalDivider(color = RadixTheme.colors.gray4)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.gray5)
                    .padding(vertical = RadixTheme.dimensions.paddingLarge, horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.mobileConnect_interactionSuccess),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletingContent(
    modifier: Modifier = Modifier,
    transactionId: IntentHash?
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            alpha = 0.2F,
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.transactionStatus_completing_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
        if (transactionId != null) {
            TransactionId(transactionId = transactionId)
        }
        Spacer(Modifier.height(36.dp))
    }
}

internal class MobileConnectParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun SuccessBottomDialogPreview(
    @PreviewParameter(MobileConnectParameterProvider::class) isMobileConnect: Boolean
) {
    RadixWalletTheme {
        SuccessContent(
            transactionId = IntentHash.sample(),
            isMobileConnect = isMobileConnect
        )
    }
}

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun CompletingBottomDialogPreview() {
    RadixWalletTheme {
        CompletingContent(transactionId = IntentHash.sample())
    }
}
