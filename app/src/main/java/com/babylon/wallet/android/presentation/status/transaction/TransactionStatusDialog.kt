package com.babylon.wallet.android.presentation.status.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.SomethingWentWrongDialogContent
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = interactionSource, indication = null) { dismissHandler() }
    ) {
        BoxWithConstraints(Modifier.align(Alignment.BottomCenter)) {
            val maxHeight = with(LocalDensity.current) {
                maxHeight.toPx()
            }
            val swipeableState = rememberSwipeableState(initialValue = SwipeState.Expanded)
            LaunchedEffect(swipeableState) {
                snapshotFlow {
                    swipeableState.currentValue
                }.distinctUntilChanged().collect {
                    if (it == SwipeState.Collapsed) {
                        dismissHandler()
                    }
                }
            }
            Column(
                modifier = Modifier
                    .applyIf(
                        !state.blockUntilComplete,
                        Modifier
                            .swipeable(
                                state = swipeableState,
                                anchors = mapOf(
                                    0f to SwipeState.Expanded,
                                    maxHeight to SwipeState.Collapsed
                                ),
                                orientation = Orientation.Vertical,
                            )
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = swipeableState.offset.value
                                        .roundToInt()
                                        .coerceIn(0, maxHeight.roundToInt())
                                )
                            }
                    )
                    .animateContentSize()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopMedium)
                    .clip(RadixTheme.shapes.roundedRectTopMedium)

            ) {
                if (!state.blockUntilComplete) {
                    BottomDialogDragHandle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RadixTheme.dimensions.paddingSmall),
                        onDismissRequest = {
                            dismissHandler()
                        }
                    )
                }
                Box {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isCompleting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CompletingContent()
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isFailed,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val title = when (state.walletErrorType) {
                            WalletErrorType.SubmittedTransactionHasFailedTransactionStatus -> {
                                stringResource(id = R.string.transaction_status_failed_title)
                            }

                            WalletErrorType.SubmittedTransactionHasPermanentlyRejectedTransactionStatus -> {
                                stringResource(id = R.string.transaction_status_rejected_title)
                            }

                            WalletErrorType.SubmittedTransactionHasTemporarilyRejectedTransactionStatus -> {
                                stringResource(id = R.string.transaction_status_error_title)
                            }

                            else -> {
                                stringResource(id = R.string.common_somethingWentWrong)
                            }
                        }
                        SomethingWentWrongDialogContent(
                            title = title,
                            subtitle = state.failureError,
                            transactionAddress = state.transactionId
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isSuccess,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        // Need to send the correct transaction id
                        SuccessContent(transactionAddress = state.transactionId)
                    }
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
                        text = {
                            Text(text = stringResource(id = R.string.transaction_status_dismiss_dialog_message))
                        },
                        confirmText = stringResource(id = R.string.common_ok),
                        dismissText = null
                    )
                }
            }
        }
    }
}

enum class SwipeState {
    Expanded, Collapsed
}

@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    transactionAddress: String
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
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.transaction_status_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.transaction_status_success_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        if (transactionAddress.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.transaction_status_transaction_id_text),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                ActionableAddressView(
                    address = transactionAddress,
                    textStyle = RadixTheme.typography.body1Regular,
                    textColor = RadixTheme.colors.gray1
                )
            }
        }
    }
}

@Composable
private fun CompletingContent(
    modifier: Modifier = Modifier
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
            text = stringResource(R.string.transaction_status_completing_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.height(36.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SuccessBottomDialogPreview() {
    RadixWalletTheme {
        SuccessContent(transactionAddress = "txid_tdx_21_1nsdfruuw5gd6tsh07ur5mgq4tjpns9vxj0nnaahaxpxmxapjzrfqmfzr4s")
    }
}

@Preview(showBackground = true)
@Composable
fun CompletingBottomDialogPreview() {
    RadixWalletTheme {
        CompletingContent()
    }
}
