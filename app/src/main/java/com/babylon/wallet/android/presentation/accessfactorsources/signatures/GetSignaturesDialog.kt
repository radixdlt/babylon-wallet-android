@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.accessfactorsources.signatures

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcePurpose
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput.ToSign.Purpose
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessArculusCardFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessDeviceFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessLedgerHardwareWalletFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessOffDeviceMnemonicFactorSourceContent
import com.babylon.wallet.android.presentation.accessfactorsources.composables.AccessPasswordFactorSourceContent
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
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
fun GetSignaturesDialog(
    modifier: Modifier = Modifier,
    viewModel: GetSignaturesViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onDismiss()
    }

    state.accessState.errorMessage?.let { errorMessage ->
        BasicPromptAlertDialog(
            finish = { viewModel.onMessageShown() },
            messageText = errorMessage.getMessage(),
            dismissText = null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                GetSignaturesViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    GetSignaturesBottomSheetContent(
        modifier = modifier,
        state = state,
        onInputConfirmed = viewModel::onInputConfirmed,
        onDismiss = viewModel::onDismiss,
        onSeedPhraseWordChanged = viewModel::onSeedPhraseWordChanged,
        onPasswordTyped = viewModel::onPasswordTyped,
        onRetryClick = viewModel::onRetry,
        onSkipClick = viewModel::onSkip
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GetSignaturesBottomSheetContent(
    modifier: Modifier = Modifier,
    state: GetSignaturesViewModel.State,
    onSeedPhraseWordChanged: (Int, String) -> Unit,
    onPasswordTyped: (String) -> Unit,
    onInputConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    onRetryClick: () -> Unit,
    onSkipClick: () -> Unit
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
    val focusedWordIndex = remember {
        mutableStateOf<Int?>(null)
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
        heightFraction = 0.8f,
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
                            wordAutocompleteCandidates =
                                accessFactorSourceState.seedPhraseInputState.delegateState.wordAutocompleteCandidates,
                            onCandidateClick = { candidate ->
                                focusedWordIndex.value?.let { index ->
                                    onSeedPhraseWordChanged(index, candidate)

                                    if (focusedWordIndex.value != accessFactorSourceState.seedPhraseInputState.delegateState.seedPhraseWords.lastIndex) {
                                        focusedWordIndex.value = index + 1
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                            }
                        )
                    }
                },
                containerColor = RadixTheme.colors.defaultBackground,
                content = { padding ->
                    val purpose = remember(state.signPurpose) { state.signPurpose.toAccessFactorSourcePurpose() }

                    val contentModifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())

                    when (accessFactorSourceState.factorSourceToAccess.kind) {
                        FactorSourceKind.DEVICE -> AccessDeviceFactorSourceContent(
                            modifier = contentModifier,
                            purpose = purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Device)?.value,
                            isRetryEnabled = accessFactorSourceState.isRetryEnabled,
                            canUseDifferentFactor = true,
                            onRetryClick = onRetryClick,
                            onSkipClick = onSkipClick
                        )

                        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> AccessLedgerHardwareWalletFactorSourceContent(
                            modifier = contentModifier,
                            purpose = purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Ledger)?.value,
                            isRetryEnabled = accessFactorSourceState.isRetryEnabled,
                            canUseDifferentFactor = true,
                            onRetryClick = onRetryClick,
                            onSkipClick = onSkipClick
                        )

                        FactorSourceKind.OFF_DEVICE_MNEMONIC -> AccessOffDeviceMnemonicFactorSourceContent(
                            modifier = contentModifier,
                            purpose = purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.OffDeviceMnemonic)?.value,
                            seedPhraseInputState = accessFactorSourceState.seedPhraseInputState,
                            canUseDifferentFactor = true,
                            focusedWordIndex = focusedWordIndex,
                            onWordChanged = onSeedPhraseWordChanged,
                            onConfirmed = onInputConfirmed,
                            onSkipClick = onSkipClick
                        )

                        FactorSourceKind.ARCULUS_CARD -> AccessArculusCardFactorSourceContent(
                            modifier = contentModifier,
                            purpose = purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.ArculusCard)?.value,
                            canUseDifferentFactor = true,
                            onSkipClick = onSkipClick
                        )

                        FactorSourceKind.PASSWORD -> AccessPasswordFactorSourceContent(
                            modifier = contentModifier,
                            purpose = purpose,
                            factorSource = (accessFactorSourceState.factorSource as? FactorSource.Password)?.value,
                            passwordState = accessFactorSourceState.passwordState,
                            onPasswordTyped = onPasswordTyped,
                            canUseDifferentFactor = true,
                            onSkipClick = onSkipClick
                        )

                        FactorSourceKind.SECURITY_QUESTIONS -> {}
                        FactorSourceKind.TRUSTED_CONTACT -> {}
                    }
                }
            )
        }
    )
}

private fun Purpose.toAccessFactorSourcePurpose() = when (this) {
    Purpose.TransactionIntents,
    Purpose.SubIntents -> AccessFactorSourcePurpose.SignatureRequest

    Purpose.AuthIntents -> AccessFactorSourcePurpose.ProvingOwnership
}

@UsesSampleValues
@Preview
@Composable
private fun GetSignaturesPreview(
    @PreviewParameter(provider = GetSignaturesPreviewParameterProvider::class) sample: Pair<Purpose, FactorSource>
) {
    RadixWalletPreviewTheme {
        GetSignaturesBottomSheetContent(
            state = GetSignaturesViewModel.State(
                signPurpose = sample.first,
                accessState = AccessFactorSourceDelegate.State(
                    factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(factorSource = sample.second),
                    seedPhraseInputState = remember(sample.second) {
                        AccessFactorSourceDelegate.State.SeedPhraseInputState(
                            delegateState = SeedPhraseInputDelegate.State()
                        )
                    }
                ),
            ),
            onDismiss = {},
            onSeedPhraseWordChanged = { _, _ -> },
            onPasswordTyped = {},
            onRetryClick = {},
            onInputConfirmed = {},
            onSkipClick = {}
        )
    }
}

@UsesSampleValues
private class GetSignaturesPreviewParameterProvider : PreviewParameterProvider<Pair<Purpose, FactorSource>> {

    private val samples: List<Pair<Purpose, FactorSource>>

    init {
        val factorSources = listOf(
            DeviceFactorSource.sample().asGeneral(),
            LedgerHardwareWalletFactorSource.sample().asGeneral(),
            ArculusCardFactorSource.sample().asGeneral(),
            OffDeviceMnemonicFactorSource.sample().asGeneral(),
            PasswordFactorSource.sample().asGeneral()
        )

        samples = Purpose.entries.flatMap { purpose ->
            factorSources.map { factorSource ->
                purpose to factorSource
            }
        }
    }

    override val values: Sequence<Pair<Purpose, FactorSource>>
        get() = samples.asSequence()
}
