package com.babylon.wallet.android.presentation.ui.composables

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.mailReportMessage
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.openEmail
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * use this if you want AlertDialog style usage, like BasicPromptAlertDialog - not using new route
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheetDialogWrapper(
    modifier: Modifier = Modifier,
    dragToDismissEnabled: Boolean = true,
    addScrim: Boolean = false,
    showDragHandle: Boolean = false,
    showDefaultTopBar: Boolean = true,
    sheetBackgroundColor: Color = RadixTheme.colors.background,
    headerBackIcon: ImageVector = Icons.Filled.Clear,
    isDismissible: Boolean = true,
    title: String? = null,
    heightFraction: Float = 1f,
    centerContent: Boolean = false,
    onDismiss: () -> Unit,
    onHeaderBackIconClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()
    val draggableState = remember(decay) {
        AnchoredDraggableState(
            initialValue = DragState.Collapsed,
            anchors = DraggableAnchors {
                DragState.Expanded at 0f
                DragState.Collapsed at 0f
            },
            positionalThreshold = { distance: Float -> distance * 0.9f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decay,
        )
    }
    val onDismissRequest = remember(isDismissible) {
        {
            if (isDismissible) {
                scope.launch {
                    draggableState.animateTo(DragState.Collapsed)
                }
            } else {
                // Delegate the dismiss request logic to the caller
                onDismiss()
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .applyIf(addScrim, Modifier.background(Color.Black.copy(alpha = 0.4f)))
            .clickable(interactionSource = interactionSource, indication = null) {
                onDismissRequest()
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            val contentMaxHeight = with(LocalDensity.current) {
                maxHeight.toPx()
            }
            draggableState.updateAnchors(
                DraggableAnchors {
                    DragState.Expanded at 0f
                    DragState.Collapsed at contentMaxHeight * 2
                }
            )
            LaunchedEffect(draggableState) {
                snapshotFlow {
                    draggableState.currentValue
                }.distinctUntilChanged().collect {
                    if (it == DragState.Collapsed) {
                        onDismiss()
                    }
                }
            }
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { /* Disable content click */ }
                    .applyIf(heightFraction != 1f, Modifier.height(maxHeight * heightFraction))
                    .applyIf(
                        dragToDismissEnabled,
                        Modifier
                            .fillMaxWidth()
                            .anchoredDraggable(
                                state = draggableState,
                                orientation = Orientation.Vertical,
                            )
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = draggableState
                                        .requireOffset()
                                        .roundToInt()
                                        .coerceIn(0, contentMaxHeight.roundToInt())
                                )
                            }
                    )
                    .animateContentSize()
                    .background(
                        color = sheetBackgroundColor,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
                    .clip(RadixTheme.shapes.roundedRectTopMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (showDefaultTopBar) {
                        BottomDialogHeader(
                            modifier = Modifier
                                .padding(top = RadixTheme.dimensions.paddingMedium)
                                .fillMaxWidth()
                                .background(
                                    color = sheetBackgroundColor,
                                    shape = RadixTheme.shapes.roundedRectTopDefault
                                ),
                            backIcon = headerBackIcon,
                            onDismissRequest = {
                                onHeaderBackIconClick?.invoke() ?: onDismissRequest()
                            },
                            title = title
                        )
                    }
                    if (showDragHandle) {
                        DefaultModalSheetDragHandle(
                            padding = PaddingValues(
                                top = RadixTheme.dimensions.paddingSmall,
                                bottom = RadixTheme.dimensions.paddingMedium
                            )
                        )
                    }
                }

                if (centerContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        content()
                    }
                } else {
                    content()
                }
            }
        }
    }
}

enum class DragState {
    Expanded, Collapsed
}

@Composable
fun BottomDialogHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    backIcon: ImageVector = Icons.Filled.Clear,
    onDismissRequest: () -> Unit
) {
    Box(modifier = modifier) {
        IconButton(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            onClick = onDismissRequest
        ) {
            Icon(
                imageVector = backIcon,
                tint = RadixTheme.colors.icon,
                contentDescription = "clear"
            )
        }
        if (title != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                text = title,
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
        }
    }
}

@Composable
fun BasicPromptAlertDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    title: (@Composable () -> Unit)? = null,
    message: (@Composable () -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.primaryButton,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { finish(false) },
        confirmButton = {
            RadixTextButton(
                text = confirmText,
                onClick = { finish(true) },
                contentColor = confirmTextColor,
                textAlign = TextAlign.End
            )
        },
        dismissButton = dismissText?.let {
            {
                RadixTextButton(
                    text = it,
                    onClick = { finish(false) },
                    textAlign = TextAlign.End
                )
            }
        },
        title = title,
        text = message,
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.background,
        properties = properties
    )
}

@Composable
fun ErrorAlertDialog(
    modifier: Modifier = Modifier,
    cancel: () -> Unit,
    title: String? = null,
    cancelMessage: String = stringResource(R.string.common_cancel),
    errorMessage: UiMessage.ErrorMessage
) {
    val commonException = remember(errorMessage) {
        errorMessage.error as? CommonException
    }

    if (commonException is CommonException.NfcSessionCancelled ||
        commonException is CommonException.HostInteractionAborted
    ) {
        return
    }

    val context = LocalContext.current
    AlertDialog(
        modifier = modifier,
        onDismissRequest = cancel,
        confirmButton = {
            RadixTextButton(
                text = cancelMessage,
                onClick = cancel,
                textAlign = TextAlign.End
            )
        },
        dismissButton = commonException?.let { error ->
            {
                RadixTextButton(
                    text = stringResource(R.string.error_emailSupportButtonTitle),
                    onClick = {
                        context.openEmail(
                            recipientAddress = Constants.RADIX_SUPPORT_EMAIL_ADDRESS,
                            subject = Constants.RADIX_SUPPORT_EMAIL_SUBJECT,
                            body = error.mailReportMessage()
                        )
                        cancel()
                    },
                    textAlign = TextAlign.End
                )
            }
        },
        title = (title ?: errorMessage.getTitle())?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )
            }
        },
        text = errorMessage.getMessage().takeIf { it.isNotBlank() }?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            }
        },
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.background
    )
}

@Composable
fun BasicPromptAlertDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    titleText: String? = null,
    messageText: String? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.primaryButton
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = finish,
        title = titleText?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )
            }
        },
        message = messageText?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            }
        },
        confirmText = confirmText,
        dismissText = dismissText,
        confirmTextColor = confirmTextColor
    )
}

@Composable
fun NoMnemonicAlertDialog(onDismiss: () -> Unit) {
    BasicPromptAlertDialog(
        finish = { onDismiss() },
        titleText = stringResource(id = R.string.common_noMnemonicAlert_title),
        messageText = stringResource(id = R.string.common_noMnemonicAlert_text),
        dismissText = null
    )
}

@Composable
fun NotSecureAlertDialog(
    finish: (accepted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = finish,
        title = {
            Text(
                text = stringResource(id = R.string.biometrics_deviceNotSecureAlert_title),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.text
            )
        },
        message = {
            Text(
                text = stringResource(id = R.string.biometrics_deviceNotSecureAlert_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
        },
        confirmText = stringResource(id = R.string.biometrics_deviceNotSecureAlert_openSettings),
        dismissText = stringResource(id = R.string.biometrics_deviceNotSecureAlert_quit)
    )
}

@Composable
fun FailureDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    transactionId: TransactionIntentHash?,
    isMobileConnect: Boolean
) {
    Column(
        modifier = modifier
            .background(
                color = if (isMobileConnect) {
                    RadixTheme.colors.backgroundSecondary
                } else {
                    RadixTheme.colors.background
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Icon(
                modifier = Modifier.size(104.dp),
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                ),
                contentDescription = null,
                tint = RadixTheme.colors.warning
            )
            Text(
                text = title,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )
            }

            if (transactionId != null) {
                TransactionId(transactionId = transactionId)
            }
        }
        if (isMobileConnect) {
            HorizontalDivider(color = RadixTheme.colors.divider)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.backgroundSecondary)
                    .padding(
                        vertical = RadixTheme.dimensions.paddingLarge,
                        horizontal = RadixTheme.dimensions.paddingXLarge
                    ),
                text = stringResource(id = R.string.mobileConnect_interactionSuccess),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal class MobileConnectParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}

@Composable
@UsesSampleValues
@Preview
private fun SomethingWentWrongDialogPreview(@PreviewParameter(MobileConnectParameterProvider::class) isMobileConnect: Boolean) {
    RadixWalletTheme {
        FailureDialogContent(
            isMobileConnect = isMobileConnect,
            title = "Title",
            subtitle = "Subtitle",
            transactionId = TransactionIntentHash.sample()
        )
    }
}
