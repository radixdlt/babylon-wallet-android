package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWrapper(
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismissRequest: () -> Unit,
    bottomSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    // TODO update dependency when this issue is resolved
    // https://issuetracker.google.com/issues/268432129
    ModalBottomSheet(
        modifier = modifier,
        sheetState = bottomSheetState,
        onDismissRequest = onDismissRequest,
        shape = RadixTheme.shapes.roundedRectTopDefault,
        dragHandle = {
            BottomDialogDragHandle(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                    .padding(vertical = RadixTheme.dimensions.paddingSmall),
                title = title,
                onDismissRequest = onDismissRequest
            )
        }
    ) {
        content()
    }
}

/**
 * use this if you want AlertDialog style usage, like BasicPromptAlertDialog - not using new route
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheetDialogWrapper(
    modifier: Modifier = Modifier,
    dragToDismissEnabled: Boolean = true,
    addScrim: Boolean = false,
    title: String? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .applyIf(addScrim, Modifier.background(Color.Black.copy(alpha = 0.4f)))
    ) {
        BoxWithConstraints(Modifier.align(Alignment.BottomCenter)) {
            val maxHeight = with(LocalDensity.current) {
                maxHeight.toPx()
            }
            val density = LocalDensity.current
            val draggableState = remember {
                AnchoredDraggableState(
                    initialValue = DragState.Expanded,
                    positionalThreshold = { distance: Float -> distance * 0.4f },
                    velocityThreshold = { with(density) { 100.dp.toPx() } },
                    animationSpec = tween(),
                    anchors = DraggableAnchors {
                        DragState.Expanded at 0f
                        DragState.Collapsed at maxHeight
                    }
                )
            }
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
                    .applyIf(
                        dragToDismissEnabled,
                        Modifier
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
                                        .coerceIn(0, maxHeight.roundToInt())
                                )
                            }
                    )
                    .animateContentSize()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopMedium)
                    .clip(RadixTheme.shapes.roundedRectTopMedium)

            ) {
                if (dragToDismissEnabled) {
                    BottomDialogDragHandle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                            .padding(vertical = RadixTheme.dimensions.paddingSmall),
                        onDismissRequest = onDismiss,
                        title = title
                    )
                }
                content()
            }
        }
    }
}

enum class DragState {
    Expanded, Collapsed
}

@Composable
fun BottomDialogDragHandle(
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismissRequest: () -> Unit
) {
    Box(modifier = modifier) {
        IconButton(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            onClick = onDismissRequest
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                tint = RadixTheme.colors.gray1,
                contentDescription = "clear"
            )
        }
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .size(38.dp, 4.dp)
                .background(color = RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
        )
        if (title != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                text = title,
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Header
            )
        }
    }
}

@Composable
fun BasicPromptAlertDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.blue2
) {
    AlertDialog(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall),
        onDismissRequest = { finish(false) },
        confirmButton = {
            RadixTextButton(text = confirmText, onClick = { finish(true) }, contentColor = confirmTextColor)
        },
        dismissButton = dismissText?.let {
            {
                RadixTextButton(text = it, onClick = { finish(false) })
            }
        },
        title = title,
        text = text
    )
}

@Composable
fun BasicPromptAlertDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    title: String? = null,
    text: String? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.blue2
) {
    AlertDialog(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall),
        onDismissRequest = { finish(false) },
        confirmButton = {
            RadixTextButton(text = confirmText, onClick = { finish(true) }, contentColor = confirmTextColor)
        },
        dismissButton = dismissText?.let {
            {
                RadixTextButton(text = it, onClick = { finish(false) })
            }
        },
        title = title?.let {
            {
                Text(
                    text = title,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            }
        },
        text = text?.let {
            {
                Text(
                    text = text,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            }
        }
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
                color = RadixTheme.colors.gray1
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.biometrics_deviceNotSecureAlert_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        },
        confirmText = stringResource(id = R.string.biometrics_deviceNotSecureAlert_openSettings),
        dismissText = stringResource(id = R.string.biometrics_deviceNotSecureAlert_quit)
    )
}

@Composable
fun SomethingWentWrongDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
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
        Icon(
            modifier = Modifier.size(104.dp),
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            ),
            contentDescription = null,
            tint = RadixTheme.colors.orange1
        )
        Text(
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        )

        subtitle?.let {
            Text(
                text = it,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }

        if (transactionAddress.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.transactionStatus_transactionID_text),
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
@Preview
fun SomethingWentWrongDialogPreview() {
    RadixWalletTheme {
        SomethingWentWrongDialogContent(
            title = "Title",
            subtitle = "Subtitle",
            transactionAddress = "rdx1239j329fj292r32e23"
        )
    }
}
