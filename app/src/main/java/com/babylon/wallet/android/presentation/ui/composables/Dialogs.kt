package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.main.OlympiaErrorState
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
            BottomDialogHeader(
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
    showDragHandle: Boolean = false,
    showDefaultTopBar: Boolean = true,
    title: String? = null,
    heightFraction: Float = 1f,
    centerContent: Boolean = false,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragState.Collapsed,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
            anchors = DraggableAnchors {
                DragState.Expanded at 0f
                DragState.Collapsed at 0f
            }
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .applyIf(addScrim, Modifier.background(Color.Black.copy(alpha = 0.4f)))
            .clickable(interactionSource = interactionSource, indication = null) {
                scope.launch {
                    draggableState.animateTo(DragState.Collapsed)
                }
            }
    ) {
        BoxWithConstraints(Modifier.align(Alignment.BottomCenter)) {
            val contentMaxHeight = with(LocalDensity.current) {
                maxHeight.toPx()
            }
            draggableState.updateAnchors(
                DraggableAnchors {
                    DragState.Expanded at 0f
                    DragState.Collapsed at contentMaxHeight
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
                    .clickable(interactionSource = interactionSource, indication = null) { /* Disable content click */ }
                    .applyIf(heightFraction != 1f, Modifier.height(maxHeight * heightFraction))
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
                                        .coerceIn(0, contentMaxHeight.roundToInt())
                                )
                            }
                    )
                    .animateContentSize()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopMedium)
                    .clip(RadixTheme.shapes.roundedRectTopMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showDragHandle) {
                    DefaultModalSheetDragHandle()
                }
                if (showDefaultTopBar) {
                    BottomDialogHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault),
                        onDismissRequest = {
                            scope.launch {
                                draggableState.animateTo(DragState.Collapsed)
                            }
                        },
                        title = title
                    )
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
                tint = RadixTheme.colors.gray1,
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
    message: (@Composable () -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.blue2
) {
    AlertDialog(
        modifier = modifier,
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
        text = message,
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.defaultBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BDFSErrorDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    title: String,
    message: String,
    state: OlympiaErrorState
) {
    BasicAlertDialog(modifier = modifier.clip(RadixTheme.shapes.roundedRectMedium), onDismissRequest = { finish(false) }) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
                .padding(RadixTheme.dimensions.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            val confirmText = if (state.isCountdownActive) {
                stringResource(id = R.string.homePage_profileOlympiaError_okCountdown, state.secondsLeft)
            } else {
                stringResource(
                    id = R.string.common_ok
                )
            }
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = message,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
            if (state.affectedAccounts.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.homePage_profileOlympiaError_affectedAccounts),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1
                )
                state.affectedAccounts.forEach { account ->
                    ActionableAddressView(
                        address = Address.init(account.address.string),
                        textStyle = RadixTheme.typography.body1Header,
                        textColor = RadixTheme.colors.blue1,
                        iconColor = RadixTheme.colors.gray2
                    )
                }
            }
            if (state.affectedPersonas.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.homePage_profileOlympiaError_affectedPersonas),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1
                )
                state.affectedPersonas.forEach { persona ->
                    ActionableAddressView(
                        address = Address.init(persona.address.string),
                        textStyle = RadixTheme.typography.body1Header,
                        textColor = RadixTheme.colors.blue1,
                        iconColor = RadixTheme.colors.gray2
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                RadixTextButton(text = confirmText, onClick = {
                    finish(true)
                }, enabled = !state.isCountdownActive)
            }
        }
    }
}

@Composable
fun BasicPromptAlertDialog(
    modifier: Modifier = Modifier,
    finish: (accepted: Boolean) -> Unit,
    titleText: String? = null,
    messageText: String? = null,
    confirmText: String = stringResource(id = R.string.common_confirm),
    dismissText: String? = stringResource(id = R.string.common_cancel),
    confirmTextColor: Color = RadixTheme.colors.blue2
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = finish,
        title = titleText?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            }
        },
        message = messageText?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
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
        titleText = stringResource(id = R.string.transactionReview_noMnemonicError_title),
        messageText = stringResource(id = R.string.transactionReview_noMnemonicError_text),
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
                color = RadixTheme.colors.gray1
            )
        },
        message = {
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
fun FailureDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
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
fun TransactionId(modifier: Modifier = Modifier, transactionId: IntentHash) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.transactionStatus_transactionID_text),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
        ActionableAddressView(
            transactionId = transactionId,
            textStyle = RadixTheme.typography.body1Header,
            textColor = RadixTheme.colors.blue1,
            iconColor = RadixTheme.colors.gray2
        )
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
            transactionId = IntentHash.sample()
        )
    }
}
