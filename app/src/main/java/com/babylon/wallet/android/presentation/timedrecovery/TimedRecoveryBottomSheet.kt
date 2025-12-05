package com.babylon.wallet.android.presentation.timedrecovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.Green1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.composables.ShieldConfigView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.utils.TimeFormatter
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.newSecurityStructureOfFactorSourcesSample
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimedRecoveryBottomSheet(
    viewModel: TimedRecoveryViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TimedRecoveryContent(
        state = state,
        onDismiss = onDismiss,
        onMessageShown = viewModel::onMessageShown,
        onConfirmClick = viewModel::onConfirmClick,
        onStopClick = viewModel::onStopClick,
        onInfoClick = onInfoClick
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
    onInfoClick: (GlossaryItem) -> Unit,
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
        windowInsets = { WindowInsets.none },
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        title = "Timed Recovery", // TODO crowdin
                        onBackClick = onDismissRequest,
                        windowInsets = WindowInsets.none,
                        backIconType = BackIconType.Close
                    )
                },
                bottomBar = {
                    if (!state.isLoading) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = RadixTheme.dimensions.paddingDefault)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(
                                color = RadixTheme.colors.divider
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                    .padding(top = RadixTheme.dimensions.paddingDefault),
                                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                            ) {
                                RadixSecondaryButton(
                                    modifier = Modifier.weight(1f),
                                    text = "Cancel Recovery", // TODO crowdin
                                    onClick = onStopClick,
                                    contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingSmall),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (state.isConfirmAvailable) {
                                    RadixSecondaryButton(
                                        modifier = Modifier.weight(1f),
                                        text = "Confirm Recovery", // TODO crowdin
                                        onClick = onConfirmClick,
                                        enabled = state.isConfirmEnabled,
                                        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingSmall),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = RadixTheme.colors.background
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.isRecoveryProposalUnknown) {
                        WarningView(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            title = "Unauthorized Recovery Detected. If you didn't start this recovery, cancel it now to protect your account.", // TODO crowdin
                        )

                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }

                    SectionView(
                        title = "Recovery Timeline", // TODO crowdin
                    ) {
                        Column {
                            state.confirmationDate?.let {
                                ConfirmationDateView(
                                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                    date = it
                                )

                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                            }

                            if (state.isConfirmEnabled) {
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                                ReadyToConfirmView(
                                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                )
                            } else {
                                state.remainingTime?.let {
                                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                                    RemainingTimeView(
                                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                        remainingTime = it
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                            HorizontalDivider()

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                            Text(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                text = "About Timed Recovery", // TODO crowdin
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.text
                            )

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            Text(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                text = "Timed recovery allows you to regain access to your account if you've lost your security factors. The waiting period protects you by giving time to cancel unauthorized recovery attempts.", // TODO crowdin
                                style = RadixTheme.typography.body3Regular,
                                color = RadixTheme.colors.text
                            )
                        }
                    }

                    if (state.isRecoveryProposalUnknown) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                        WarningView(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            title = "Unrecognized Recovery",
                            text = "You cannot see what security factors will control this account after recovery completes.", // TODO crowdin
                            iconColor = RadixTheme.colors.icon
                        )
                    } else {
                        state.securityStructure?.let { structure ->
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            SectionView(
                                title = "New Security Factors", // TODO crowdin
                                isCollapsible = true
                            ) {
                                Column {
                                    Text(
                                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                        text = "After confirmation, your account will be secured with:", // TODO crowdin
                                        style = RadixTheme.typography.body3Regular,
                                        color = RadixTheme.colors.text
                                    )

                                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                                    ShieldConfigView(
                                        modifier = Modifier
                                            .padding(
                                                horizontal = RadixTheme.dimensions.paddingDefault
                                            )
                                            .background(
                                                color = RadixTheme.colors.background,
                                                shape = RadixTheme.shapes.roundedRectDefault
                                            ),
                                        securityStructure = structure,
                                        onInfoClick = onInfoClick
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    SectionView(
                        title = "What Happens Next?" // TODO crowdin
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = if (state.isRecoveryProposalUnknown) {
                                "Confirm is **unavailable. You can only cancel recoveries you don't recognize to protect your account.**\nCancel to stop the recovery and keep your current security setup."
                                    .formattedSpans(SpanStyle(color = RadixTheme.colors.error)) // TODO crowdin
                            } else {
                                AnnotatedString(
                                    "Confirm to complete the recovery and switch to the new security factors.\nCancel to stop the recovery and keep your current security setup."
                                ) // TODO crowdin
                            },
                            style = RadixTheme.typography.body3Regular,
                            color = RadixTheme.colors.text
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SectionView(
    title: String,
    isCollapsible: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .background(
                color = RadixTheme.colors.backgroundSecondary,
                shape = RadixTheme.shapes.roundedRectDefault
            )
    ) {
        var isExpanded by remember { mutableStateOf(true) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.backgroundSecondary,
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                .noIndicationClickable(enabled = isCollapsible) {
                    isExpanded = !isExpanded
                }
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text
            )

            if (isCollapsible) {
                Icon(
                    painter = painterResource(
                        id = if (isExpanded) {
                            R.drawable.ic_arrow_up
                        } else {
                            R.drawable.ic_arrow_down
                        }
                    ),
                    contentDescription = null,
                    tint = RadixTheme.colors.iconSecondary
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@Composable
private fun ConfirmationDateView(
    date: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.ic_calendar),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )

        Column {
            Text(
                text = "Confirmable after",
                style = RadixTheme.typography.body3Regular,
                color = RadixTheme.colors.textSecondary
            )

            Text(
                text = date,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text
            )
        }
    }
}

@Composable
private fun ReadyToConfirmView(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_circle),
            contentDescription = null,
            tint = Green1
        )

        Text(
            text = "Ready to confirm",
            style = RadixTheme.typography.body2HighImportance,
            color = Green1
        )
    }
}

@Composable
private fun RemainingTimeView(
    remainingTime: Duration,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(id = R.drawable.hourglass),
            contentDescription = null,
            tint = RadixTheme.colors.text
        )

        Column {
            Text(
                text = "Time remaining",
                style = RadixTheme.typography.body3Regular,
                color = RadixTheme.colors.textSecondary
            )

            val time = remember(remainingTime) {
                TimeFormatter.formatShortTruncatedToHours(remainingTime)
            }

            Text(
                text = time,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.text
            )
        }
    }
}

@Composable
private fun WarningView(
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    textColor: Color = RadixTheme.colors.text,
    iconColor: Color = RadixTheme.colors.error
) {
    Row(
        modifier = modifier.then(
            Modifier
                .background(
                    shape = RadixTheme.shapes.roundedRectDefault,
                    color = RadixTheme.colors.errorSecondary
                )
                .padding(RadixTheme.dimensions.paddingDefault)
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_warning_error),
            contentDescription = null,
            tint = iconColor
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.error
            )

            text?.let {
                Text(
                    text = it,
                    style = RadixTheme.typography.body3Regular,
                    color = textColor
                )
            }
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun TimedRecoveryPreview(
    @PreviewParameter(TimedRecoveryPreviewProvider::class) state: TimedRecoveryViewModel.State
) {
    RadixWalletPreviewTheme {
        TimedRecoveryContent(
            state = state,
            onDismiss = {},
            onMessageShown = {},
            onStopClick = {},
            onConfirmClick = {},
            onInfoClick = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun TimedRecoveryDarkPreview(
    @PreviewParameter(TimedRecoveryPreviewProvider::class) state: TimedRecoveryViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        TimedRecoveryContent(
            state = state,
            onDismiss = {},
            onMessageShown = {},
            onStopClick = {},
            onConfirmClick = {},
            onInfoClick = {}
        )
    }
}

class TimedRecoveryPreviewProvider : PreviewParameterProvider<TimedRecoveryViewModel.State> {

    override val values: Sequence<TimedRecoveryViewModel.State>
        get() = sequenceOf(
            TimedRecoveryViewModel.State(
                isLoading = false,
                securityStructure = newSecurityStructureOfFactorSourcesSample(),
                remainingTime = 5.minutes,
                confirmationDate = "October 23, 2025 at 12:33"
            ),
            TimedRecoveryViewModel.State(
                isLoading = false,
                isRecoveryProposalUnknown = true,
                remainingTime = 5.minutes,
                confirmationDate = "October 23, 2025 at 12:33"
            ),
            TimedRecoveryViewModel.State(
                isLoading = false,
                securityStructure = newSecurityStructureOfFactorSourcesSample(),
                confirmationDate = "October 23, 2025 at 12:33"
            )
        )
}
