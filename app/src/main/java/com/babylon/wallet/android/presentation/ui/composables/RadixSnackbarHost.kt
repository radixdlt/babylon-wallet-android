package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.Black
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.mailReportMessage
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.openEmail
import com.radixdlt.sargon.CommonException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
fun BoxScope.SnackbarUiMessageHandler(
    message: UiMessage?,
    modifier: Modifier = Modifier,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RadixSnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(RadixTheme.dimensions.paddingLarge)
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
    if (message != null) {
        val errorMessage = message.getMessage()
        val actionLabel = remember(message) {
            val supportMessage = (
                (message as? UiMessage.ErrorMessage)
                    ?.error as? CommonException
                )
                ?.mailReportMessage()

            if (supportMessage != null) {
                ActionLabel(
                    type = ActionType.CustomerSupport,
                    payload = supportMessage
                )
            } else {
                null
            }
        }

        LaunchedEffect(message.id, errorMessage, actionLabel) {
            val encodedLabel = Json.encodeToString(actionLabel)

            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = encodedLabel,
                duration = if (actionLabel != null) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                }
            )
            onMessageShown()
        }
    }
}

@Composable
fun RadixSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        RadixSnackbar(snackbarData = data)
    },
) {
    SnackbarHost(hostState = hostState, modifier = modifier, snackbar = snackbar)
}

@Composable
private fun RadixSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    shape: Shape = RadixTheme.shapes.roundedRectSmall,
) {
    val actionLabel = remember(snackbarData.visuals.actionLabel) {
        snackbarData.visuals.actionLabel?.let {
            runCatching {
                Json.decodeFromString<ActionLabel>(it)
            }.getOrNull()
        }
    }

    val context = LocalContext.current
    val (containerColor, contentColor) = if (RadixTheme.config.isDarkTheme) {
        White to Black
    } else {
        Black to White
    }

    Snackbar(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        content = {
            Text(text = snackbarData.visuals.message, style = RadixTheme.typography.body2Regular)
        },
        action = if (actionLabel != null) {
            {
                val label = when (actionLabel.type) {
                    ActionType.CustomerSupport -> stringResource(R.string.error_emailSupportButtonTitle)
                    ActionType.Ok -> stringResource(R.string.common_ok)
                }

                RadixTextButton(
                    text = label,
                    onClick = {
                        when (actionLabel.type) {
                            ActionType.CustomerSupport -> {
                                context.openEmail(
                                    recipientAddress = Constants.RADIX_SUPPORT_EMAIL_ADDRESS,
                                    subject = Constants.RADIX_SUPPORT_EMAIL_SUBJECT,
                                    body = actionLabel.payload
                                )
                                snackbarData.performAction()
                            }
                            ActionType.Ok -> {
                                snackbarData.dismiss()
                            }
                        }
                    },
                    contentColor = contentColor
                )
            }
        } else {
            null
        }
    )
}

private enum class ActionType {
    CustomerSupport,
    Ok
}

@Serializable
private data class ActionLabel(
    val type: ActionType,
    val payload: String
)
