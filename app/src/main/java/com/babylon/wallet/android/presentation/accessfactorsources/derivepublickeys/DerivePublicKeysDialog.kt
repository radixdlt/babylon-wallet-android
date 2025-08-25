package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcePurpose
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceSkipOption
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessArculusCardFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessDeviceFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessLedgerHardwareWalletFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessOffDeviceMnemonicFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessPasswordFactorSourceContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.rememberSuggestionsVisibilityState
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.PasswordFactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.launch

@Composable
fun DerivePublicKeysDialog(
    modifier: Modifier = Modifier,
    viewModel: DerivePublicKeysViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onDismiss()
    }

    state.accessState.errorMessage?.let { errorMessage ->
        ErrorAlertDialog(
            cancel = { viewModel.onMessageShown() },
            errorMessage = errorMessage
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DerivePublicKeysViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    DerivePublicKeysSheetContent(
        modifier = modifier,
        state = state,
        onSeedPhraseWordChanged = viewModel::onSeedPhraseWordChanged,
        onPasswordTyped = viewModel::onPasswordTyped,
        onArculusPinChange = viewModel::onArculusPinChange,
        onForgotArculusPinClick = viewModel::onForgotArculusPinClick,
        onArculusInfoMessageDismiss = viewModel::onArculusInfoMessageDismiss,
        onInputConfirmed = viewModel::onInputConfirmed,
        onDismiss = viewModel::onDismiss,
        onRetryClick = viewModel::onRetry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DerivePublicKeysSheetContent(
    modifier: Modifier = Modifier,
    state: DerivePublicKeysViewModel.State,
    onSeedPhraseWordChanged: (Int, String) -> Unit,
    onPasswordTyped: (String) -> Unit,
    onArculusPinChange: (String) -> Unit,
    onForgotArculusPinClick: () -> Unit,
    onArculusInfoMessageDismiss: () -> Unit,
    onInputConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    onRetryClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            sheetState.show()
        }
    }

    val accessFactorSourceState = state.accessState
    val isSeedPhraseSuggestionsVisible = accessFactorSourceState.seedPhraseInputState.delegateState.rememberSuggestionsVisibilityState()
    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
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
                bottomBar = {
                    if (isSeedPhraseSuggestionsVisible) {
                        SeedPhraseSuggestions(
                            modifier = Modifier
                                .imePadding()
                                .fillMaxWidth()
                                .height(RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight)
                                .padding(RadixTheme.dimensions.paddingSmall),
                            wordAutocompleteCandidates = accessFactorSourceState
                                .seedPhraseInputState.delegateState.wordAutocompleteCandidates,
                            onCandidateClick = { candidate ->
                                focusedWordIndex?.let { index ->
                                    onSeedPhraseWordChanged(index, candidate)
                                }
                            }
                        )
                    }
                },
                containerColor = RadixTheme.colors.background,
                content = { padding ->
                    val contentModifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())

                    when (accessFactorSourceState.factorSourceToAccess.kind) {
                        FactorSourceKind.DEVICE -> AccessDeviceFactorSourceContent(
                            modifier = contentModifier,
                            purpose = state.purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Device)?.value,
                            isRetryEnabled = accessFactorSourceState.isRetryEnabled,
                            skipOption = AccessFactorSourceSkipOption.None,
                            onRetryClick = onRetryClick,
                            onSkipClick = {}
                        )

                        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> AccessLedgerHardwareWalletFactorSourceContent(
                            modifier = contentModifier,
                            purpose = state.purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Ledger)?.value,
                            isRetryEnabled = accessFactorSourceState.isRetryEnabled,
                            skipOption = AccessFactorSourceSkipOption.None,
                            onRetryClick = onRetryClick,
                            onSkipClick = {}
                        )

                        FactorSourceKind.OFF_DEVICE_MNEMONIC -> AccessOffDeviceMnemonicFactorSourceContent(
                            modifier = contentModifier,
                            purpose = state.purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.OffDeviceMnemonic)?.value,
                            seedPhraseInputState = accessFactorSourceState.seedPhraseInputState,
                            skipOption = AccessFactorSourceSkipOption.None,
                            onWordChanged = onSeedPhraseWordChanged,
                            onConfirmed = onInputConfirmed,
                            onFocusedWordChanged = {
                                focusedWordIndex = it
                            },
                            onSkipClick = {}
                        )

                        FactorSourceKind.ARCULUS_CARD -> AccessArculusCardFactorSourceContent(
                            modifier = contentModifier,
                            purpose = state.purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.ArculusCard)?.value,
                            pinState = accessFactorSourceState.arculusPinState,
                            onPinChange = onArculusPinChange,
                            onForgotPinClick = onForgotArculusPinClick,
                            onInfoMessageDismiss = onArculusInfoMessageDismiss,
                            onRetryClick = onRetryClick,
                            skipOption = AccessFactorSourceSkipOption.None,
                            onSkipClick = {},
                            onConfirmClick = onInputConfirmed
                        )

                        FactorSourceKind.PASSWORD -> AccessPasswordFactorSourceContent(
                            modifier = contentModifier,
                            purpose = state.purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Password)?.value,
                            passwordState = accessFactorSourceState.passwordState,
                            onPasswordTyped = onPasswordTyped,
                            skipOption = AccessFactorSourceSkipOption.None,
                            onSkipClick = {}
                        )
                    }
                }
            )
        }
    )
}

@UsesSampleValues
@Preview
@Composable
private fun DerivePublicKeyPreview(
    @PreviewParameter(DerivePublicKeysPreviewParameterProvider::class) param: Pair<AccessFactorSourcePurpose, FactorSource>
) {
    RadixWalletPreviewTheme {
        DerivePublicKeysSheetContent(
            state = DerivePublicKeysViewModel.State(
                purpose = param.first,
                accessState = AccessFactorSourceDelegate.State(
                    factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                        factorSource = param.second
                    )
                )
            ),
            onDismiss = {},
            onSeedPhraseWordChanged = { _, _ -> },
            onPasswordTyped = {},
            onArculusPinChange = {},
            onForgotArculusPinClick = {},
            onArculusInfoMessageDismiss = {},
            onRetryClick = {},
            onInputConfirmed = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun DerivePublicKeyPreviewDark(
    @PreviewParameter(DerivePublicKeysPreviewParameterProvider::class) param: Pair<AccessFactorSourcePurpose, FactorSource>
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DerivePublicKeysSheetContent(
            state = DerivePublicKeysViewModel.State(
                purpose = param.first,
                accessState = AccessFactorSourceDelegate.State(
                    factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                        factorSource = param.second
                    )
                )
            ),
            onDismiss = {},
            onSeedPhraseWordChanged = { _, _ -> },
            onPasswordTyped = {},
            onArculusPinChange = {},
            onForgotArculusPinClick = {},
            onArculusInfoMessageDismiss = {},
            onRetryClick = {},
            onInputConfirmed = {}
        )
    }
}

@UsesSampleValues
class DerivePublicKeysPreviewParameterProvider : PreviewParameterProvider<Pair<AccessFactorSourcePurpose, FactorSource>> {

    override val values: Sequence<Pair<AccessFactorSourcePurpose, FactorSource>>
        get() {
            val factorSources = listOf(
                DeviceFactorSource.sample().asGeneral(),
                LedgerHardwareWalletFactorSource.sample().asGeneral(),
                ArculusCardFactorSource.sample().asGeneral(),
                OffDeviceMnemonicFactorSource.sample().asGeneral(),
                PasswordFactorSource.sample().asGeneral(),
            )

            val purposes = listOf(
                AccessFactorSourcePurpose.UpdatingFactorConfig,
                AccessFactorSourcePurpose.DerivingAccounts
            )

            return purposes.map { purpose ->
                factorSources.map { factorSource ->
                    (purpose to factorSource)
                }
            }.flatten().asSequence()
        }
}
