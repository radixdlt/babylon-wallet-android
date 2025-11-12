package com.babylon.wallet.android.presentation.timedrecovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.Pink1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.resolveTitleResFromGlossaryItem
import com.babylon.wallet.android.presentation.transaction.composables.ShieldConfigView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
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
                                    text = "Stop", // TODO crowdin
                                    onClick = onStopClick
                                )

                                RadixPrimaryButton(
                                    modifier = Modifier.weight(1.5f),
                                    text = "Confirm", // TODO crowdin
                                    onClick = onConfirmClick,
                                    enabled = state.canConfirm
                                )
                            }

                            if (state.remainingTime != null && !state.canConfirm) {
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                                RemainingTimeView(
                                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                    time = state.remainingTime
                                )
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
                        PromptLabel(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "The proposed Security Shield configuration is unknown.", // TODO crowdin
                        )

                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }

                    val text = if (state.canConfirm) {
                        "The timed recovery period is complete. You can now confirm the new Security Shield " +
                            "configuration shown below. If you've changed your mind, you can stop this process to " +
                            "discard the update."
                    } else {
                        "Your new Security Shield is in a timed recovery period. This is a security feature to " +
                            "protect your assets.\n\nYou will be able to confirm this change after:\n" +
                            "**${state.confirmationDate}**.\n\nReview the proposed configuration below. If you " +
                            "don't recognize this activity, you can stop this process immediately."
                    }

                    Text(
                        modifier = Modifier.padding(
                            horizontal = RadixTheme.dimensions.paddingDefault
                        ),
                        text = text.formattedSpans(
                            boldStyle = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = RadixTheme.colors.text
                            )
                        ),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.text
                    )

                    state.securityStructure?.let { structure ->
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .background(
                                    color = RadixTheme.colors.backgroundSecondary,
                                    shape = RadixTheme.shapes.roundedRectMedium
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
                                    .noIndicationClickable {
                                        isExpanded = !isExpanded
                                    }
                                    .padding(RadixTheme.dimensions.paddingDefault),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Proposed Shield", // TODO crowdin
                                    style = RadixTheme.typography.body1HighImportance,
                                    color = RadixTheme.colors.text
                                )

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

                            AnimatedVisibility(
                                visible = isExpanded
                            ) {
                                HorizontalDivider(
                                    color = RadixTheme.colors.divider
                                )

                                ShieldConfigView(
                                    securityStructure = structure
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                        Spacer(modifier = Modifier.weight(1f))

                        InfoButton(
                            text = stringResource(id = GlossaryItem.emergencyfallback.resolveTitleResFromGlossaryItem())
                        ) {
                            onInfoClick(GlossaryItem.emergencyfallback)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RemainingTimeView(
    time: Duration,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val time = remember(time) {
        TimeFormatter.format(context, time, time.inWholeSeconds < 60)
    }

    Box(
        modifier = modifier,
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

class TimedRecoveryPreviewProvider : PreviewParameterProvider<TimedRecoveryViewModel.State> {

    override val values: Sequence<TimedRecoveryViewModel.State>
        get() = sequenceOf(
            TimedRecoveryViewModel.State(
                isLoading = false,
                securityStructure = newSecurityStructureOfFactorSourcesSample(),
                remainingTime = 5.minutes,
                confirmationDate = "October 23, 2025 02:00 PM"
            ),
            TimedRecoveryViewModel.State(
                isLoading = false,
                isRecoveryProposalUnknown = true,
                remainingTime = 5.minutes,
                confirmationDate = "October 23, 2025 02:00 PM"
            ),
            TimedRecoveryViewModel.State(
                isLoading = false,
                securityStructure = newSecurityStructureOfFactorSourcesSample()
            )
        )
}
