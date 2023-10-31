package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.UiMessage

@Composable
fun BoxScope.SnackbarUiMessageHandler(
    message: UiMessage?,
    modifier: Modifier = Modifier,
    onMessageShown: () -> Unit,
    action: @Composable (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RadixSnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(RadixTheme.dimensions.paddingLarge),
        action = action
    )
    SnackbarUIMessage(
        message = message,
        snackbarHostState = snackbarHostState,
        onMessageShown = onMessageShown
    )
}

@Composable
fun SnackbarUIMessage(
    message: UiMessage?,
    snackbarHostState: SnackbarHostState,
    onMessageShown: () -> Unit
) {
    val messageToShow = message?.getMessage()
    messageToShow?.let {
        LaunchedEffect(messageToShow, message.id) {
            snackbarHostState.showSnackbar(message = messageToShow)
            onMessageShown()
        }
    }
}

@Composable
fun RadixSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        RadixSnackbar(snackbarData = data, action = action)
    },
) {
    SnackbarHost(hostState = hostState, modifier = modifier, snackbar = snackbar)
}

@Composable
fun RadixSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    shape: Shape = RadixTheme.shapes.roundedRectSmall,
    containerColor: Color = RadixTheme.colors.backgroundAlternate,
    contentColor: Color = RadixTheme.colors.gray3
) {
    Snackbar(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        content = {
            Text(text = snackbarData.visuals.message, style = RadixTheme.typography.body2Regular)
        },
        action = action
    )
}
