package com.babylon.wallet.android.presentation.timedrecovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.Pink1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.utils.TimeFormatter
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimedRecoveryBottomSheet(
    viewModel: TimedRecoveryViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimedRecoveryContent(
        state = state,
        onDismiss = onDismiss,
        onMessageShown = viewModel::onMessageShown,
        onConfirmClick = viewModel::onConfirmClick,
        onStopClick = viewModel::onStopClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TimedRecoveryViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimedRecoveryContent(
    state: TimedRecoveryViewModel.State,
    onDismiss: () -> Unit,
    onMessageShown: () -> Unit,
    onConfirmClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }
    val onDismissRequest: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        wrapContent = true,
        windowInsets = { WindowInsets.none },
        sheetContent = {
            Column {
                RadixCenteredTopAppBar(
                    title = "Timed Recovery", // TODO crowdin
                    onBackClick = onDismissRequest,
                    windowInsets = WindowInsets.none,
                    backIconType = BackIconType.Close
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))

                if (!state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        RadixSecondaryButton(
                            modifier = Modifier.weight(1f),
                            text = "Stop",
                            onClick = onStopClick
                        )

                        RadixPrimaryButton(
                            modifier = Modifier.weight(1f),
                            text = "Confirm",
                            onClick = onConfirmClick,
                            enabled = state.canConfirm
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))

                if (state.remainingTime == null) {
                    Box(modifier = Modifier.navigationBarsPadding())
                } else {
                    RemainingTimeView(
                        time = state.remainingTime
                    )
                }
            }
        }
    )
}

@Composable
private fun RemainingTimeView(
    time: Duration
) {
    val context = LocalContext.current

    val time = remember(time) {
        TimeFormatter.format(context, time, time.inWholeSeconds < 60)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.backgroundSecondary
            )
            .padding(RadixTheme.dimensions.paddingDefault)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Remaining time: $time", // TODO crowdin
            style = RadixTheme.typography.body2Regular,
            color = Pink1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
private fun TimedRecoveryPreview() {
    RadixWalletPreviewTheme {
        TimedRecoveryContent(
            state = TimedRecoveryViewModel.State(
                isLoading = false,
                remainingTime = 5.minutes
            ),
            onDismiss = {},
            onMessageShown = {},
            onStopClick = {},
            onConfirmClick = {}
        )
    }
}
