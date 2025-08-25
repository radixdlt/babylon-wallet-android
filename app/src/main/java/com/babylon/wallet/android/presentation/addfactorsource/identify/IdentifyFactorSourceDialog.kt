package com.babylon.wallet.android.presentation.addfactorsource.identify

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.coroutines.launch

@Composable
fun IdentifyFactorSourceDialog(
    modifier: Modifier = Modifier,
    viewModel: IdentifyFactorSourceViewModel,
    onDismiss: () -> Unit,
    onLedgerIdentified: () -> Unit,
    onArculusIdentified: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        onDismiss()
    }

    IdentifyFactorSourceContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onMessageShown = viewModel::onMessageShown,
        onArculusInfoMessageDismiss = viewModel::onArculusInfoMessageDismiss,
        onRetryClick = viewModel::onRetry
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is IdentifyFactorSourceViewModel.Event.LedgerIdentified -> onLedgerIdentified()
                IdentifyFactorSourceViewModel.Event.ArculusIdentified -> onArculusIdentified()
            }
        }
    }
}

@Composable
private fun FactorSourceKind.message() = when (this) {
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> stringResource(id = R.string.addFactorSource_ledger_identifyingInstructions)
        .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))

    FactorSourceKind.ARCULUS_CARD -> stringResource(id = R.string.addFactorSource_arculus_identifyingInstructions)
        .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))

    FactorSourceKind.DEVICE,
    FactorSourceKind.OFF_DEVICE_MNEMONIC,
    FactorSourceKind.PASSWORD -> error("Not supported here")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentifyFactorSourceContent(
    modifier: Modifier = Modifier,
    state: IdentifyFactorSourceViewModel.State,
    onDismiss: () -> Unit,
    onMessageShown: () -> Unit,
    onArculusInfoMessageDismiss: () -> Unit,
    onRetryClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    if (sheetState.hasExpandedState) {
        state.errorMessage?.let { errorMessage ->
            ErrorAlertDialog(
                cancel = { onMessageShown() },
                errorMessage = errorMessage
            )
        }

        if (state.showArculusInfoMessage) {
            BasicPromptAlertDialog(
                messageText = stringResource(id = R.string.addArculus_seedPhraseInstructions_message),
                confirmText = stringResource(id = R.string.common_ok),
                dismissText = null,
                finish = { onArculusInfoMessageDismiss() }
            )
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
        heightFraction = 0.7f,
        sheetState = sheetState,
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        windowInsets = WindowInsets.none,
                        title = "",
                        onBackClick = onDismiss,
                        backIconType = BackIconType.Close
                    )
                },
                containerColor = RadixTheme.colors.background,
                content = { padding ->
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())

                    AccessContent(
                        modifier = contentModifier,
                        title = stringResource(id = R.string.addFactorSource_identifying_title),
                        message = state.factorSourceKind.message(),
                        content = {
                            RadixTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                                    .height(50.dp),
                                text = stringResource(R.string.common_retry),
                                enabled = state.isRetryEnabled,
                                onClick = onRetryClick
                            )
                        }
                    )
                }
            )
        }
    )
}

@UsesSampleValues
@Preview
@Composable
private fun IdentifyFactorSourcePreviewLight(
    @PreviewParameter(IdentifyFactorSourceContentPreviewProvider::class) state: IdentifyFactorSourceViewModel.State
) {
    RadixWalletPreviewTheme {
        IdentifyFactorSourceContent(
            state = state,
            onDismiss = {},
            onRetryClick = {},
            onMessageShown = {},
            onArculusInfoMessageDismiss = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun IdentifyFactorSourcePreviewDark(
    @PreviewParameter(IdentifyFactorSourceContentPreviewProvider::class) state: IdentifyFactorSourceViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        IdentifyFactorSourceContent(
            state = state,
            onDismiss = {},
            onRetryClick = {},
            onMessageShown = {},
            onArculusInfoMessageDismiss = {}
        )
    }
}

@UsesSampleValues
class IdentifyFactorSourceContentPreviewProvider : PreviewParameterProvider<IdentifyFactorSourceViewModel.State> {

    override val values: Sequence<IdentifyFactorSourceViewModel.State>
        get() = sequenceOf(
            IdentifyFactorSourceViewModel.State(
                factorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
            )
        )
}
