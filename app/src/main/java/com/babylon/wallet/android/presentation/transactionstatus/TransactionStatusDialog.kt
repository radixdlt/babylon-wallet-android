package com.babylon.wallet.android.presentation.transactionstatus

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper
import com.babylon.wallet.android.presentation.ui.composables.SomethingWentWrongDialogContent
import com.babylon.wallet.android.utils.AppEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: TransactionStatusDialogViewModel,
    onBackPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dismissHandler = {
        viewModel.incomingRequestHandled()
        onBackPress()
    }
    BottomSheetWrapper(
        modifier = modifier,
        onDismissRequest = dismissHandler
    ) {
        Box(modifier = Modifier.animateContentSize()) {
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
                SomethingWentWrongDialogContent(
                    title = stringResource(id = R.string.common_somethingWentWrong),
                    subtitle = state.failureError?.getUserFriendlyDescriptionRes()?.let { stringResource(id = it) }.orEmpty()
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.isSuccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SuccessContent()
            }
        }

        Row {
            val scope = rememberCoroutineScope()
            Button(
                onClick = { scope.launch {
                    viewModel.appEventBus.sendEvent(AppEvent.TransactionEvent.Successful(state.requestId))
                } }
            ) {
                Text("Success ${state.requestId}")
            }

            Button(
                onClick = { scope.launch {
                    viewModel.appEventBus.sendEvent(
                        AppEvent.TransactionEvent.Failed(
                            state.requestId,
                            UiMessage.ErrorMessage(DappRequestFailure.TransactionApprovalFailure.BuildTransactionHeader)
                        )
                    )
                } }
            ) {
                Text("Error ${state.requestId}")
            }
        }
    }
}

@Composable
private fun SuccessContent(
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
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.transaction_status_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = stringResource(R.string.transaction_status_success_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
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
        SuccessContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CompletingBottomDialogPreview() {
    RadixWalletTheme {
        CompletingContent()
    }
}
