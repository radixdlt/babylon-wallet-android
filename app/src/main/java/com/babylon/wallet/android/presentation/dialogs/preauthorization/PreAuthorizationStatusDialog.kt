package com.babylon.wallet.android.presentation.dialogs.preauthorization

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import kotlin.time.Duration.Companion.seconds

@Composable
fun PreAuthorizationStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: PreAuthorizationStatusViewModel,
    onDismiss: () -> Unit
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
                PreAuthorizationStatusViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }

    PreAuthorizationStatusContent(
        modifier = modifier,
        state = state,
        onDismiss = dismissHandler
    )
}

@Composable
private fun PreAuthorizationStatusContent(
    modifier: Modifier = Modifier,
    state: PreAuthorizationStatusViewModel.State,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss,
        dragToDismissEnabled = true,
        showDragHandle = true,
        isDismissible = false,
        content = {
            androidx.compose.animation.AnimatedVisibility(
                visible = state.status is PreAuthorizationStatusViewModel.State.Status.Sent,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SentContent(
                    status = state.status as PreAuthorizationStatusViewModel.State.Status.Sent
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.status is PreAuthorizationStatusViewModel.State.Status.Expired,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExpiredContent(
                    status = state.status as PreAuthorizationStatusViewModel.State.Status.Expired
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.status is PreAuthorizationStatusViewModel.State.Status.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SuccessContent(
                    status = state.status as PreAuthorizationStatusViewModel.State.Status.Success
                )
            }
        }
    )
}

@Composable
private fun SentContent(
    modifier: Modifier = Modifier,
    status: PreAuthorizationStatusViewModel.State.Status.Sent
) {
}

@Composable
private fun ExpiredContent(
    modifier: Modifier = Modifier,
    status: PreAuthorizationStatusViewModel.State.Status.Expired
) {
}

@Composable
private fun SuccessContent(
    modifier: Modifier = Modifier,
    status: PreAuthorizationStatusViewModel.State.Status.Success
) {
}

@Composable
@Preview
private fun PreAuthorizationStatusPreview(
    @PreviewParameter(PreAuthorizationStatusPreviewProvider::class) state: PreAuthorizationStatusViewModel.State
) {
    PreAuthorizationStatusContent(
        state = state,
        onDismiss = {}
    )
}

class PreAuthorizationStatusPreviewProvider : PreviewParameterProvider<PreAuthorizationStatusViewModel.State> {

    override val values: Sequence<PreAuthorizationStatusViewModel.State>
        get() = sequenceOf(
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Sent(
                    preAuthorizationId = "PAid...0runll",
                    dAppName = "Collab.Fi",
                    expiration = PreAuthorizationStatusViewModel.State.Status.Sent.Expiration(
                        duration = 30.seconds
                    )
                )
            ),
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Expired(
                    isMobileConnect = true
                )
            ),
            PreAuthorizationStatusViewModel.State(
                status = PreAuthorizationStatusViewModel.State.Status.Success(
                    transactionId = "",
                    isMobileConnect = false
                )
            )
        )
}
