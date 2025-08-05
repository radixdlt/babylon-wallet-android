package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsViewModel.RecoverableFactorSource
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.rememberSuggestionsVisibilityState
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.HideKeyboardOnFullScroll
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.sargon.sample

@Composable
fun ImportMnemonicsScreen(
    viewModel: ImportMnemonicsViewModel,
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    ImportMnemonicsContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSkipSeedPhraseClick = {
            viewModel.onSkipSeedPhraseClick()
        },
        onContinueClick = {
            keyboardController?.hide()
            viewModel.onContinueClick()
        },
        onWordChanged = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onWordSelected = viewModel::onWordSelected,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is ImportMnemonicsViewModel.Event.FinishRestoration -> onDismiss(it.isMovingToMain)
                is ImportMnemonicsViewModel.Event.CloseApp -> onCloseApp()
            }
        }
    }
}

@Composable
private fun ImportMnemonicsContent(
    modifier: Modifier = Modifier,
    state: ImportMnemonicsViewModel.State,
    onBackClick: () -> Unit,
    onSkipSeedPhraseClick: () -> Unit,
    onContinueClick: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onMessageShown: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    var focusedWordIndex by remember { mutableStateOf<Int?>(null) }
    val isSuggestionsVisible = state.seedPhraseState.rememberSuggestionsVisibilityState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.uiMessage) {
        if (state.uiMessage != null) {
            keyboardController?.hide()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.enterSeedPhrase_header_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            if (isSuggestionsVisible) {
                SeedPhraseSuggestions(
                    wordAutocompleteCandidates = state.seedPhraseState.wordAutocompleteCandidates,
                    modifier = Modifier
                        .imePadding()
                        .fillMaxWidth()
                        .height(RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight)
                        .padding(RadixTheme.dimensions.paddingSmall),
                    onCandidateClick = { candidate ->
                        focusedWordIndex?.let {
                            onWordSelected(it, candidate)
                        }
                    }
                )
            } else if (!state.isLoading) {
                RadixBottomBar(
                    text = stringResource(R.string.common_continue),
                    enabled = state.isPrimaryButtonEnabled,
                    isLoading = state.isPrimaryButtonLoading,
                    onClick = onContinueClick,
                    additionalTopContent = {
                        if (state.isSecondaryButtonLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(vertical = RadixTheme.dimensions.paddingMedium)
                                    .size(20.dp),
                                color = RadixTheme.colors.icon,
                                strokeWidth = 2.dp
                            )
                        } else {
                            RadixTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                                    .padding(bottom = RadixTheme.dimensions.paddingSmall),
                                text = stringResource(id = R.string.recoverSeedPhrase_skipButton),
                                onClick = onSkipSeedPhraseClick
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        SecureScreen()

        val scrollState = rememberScrollState()
        HideKeyboardOnFullScroll(scrollState)

        if (!state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .keyboardVisiblePadding(
                        padding = padding,
                        bottom = if (isSuggestionsVisible) {
                            RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight
                        } else {
                            RadixTheme.dimensions.paddingDefault
                        }
                    )
            ) {
                state.recoverableFactorSource?.let {
                    FactorSourceCardView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        item = it.card
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                SeedPhraseInputForm(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    seedPhraseWords = state.seedPhraseState.seedPhraseWords,
                    bip39Passphrase = state.seedPhraseState.bip39Passphrase,
                    onWordChanged = onWordChanged,
                    onPassphraseChanged = onPassphraseChanged,
                    onFocusedWordIndexChanged = { focusedWordIndex = it },
                    initiallyFocusedIndex = 0
                )

                val shouldDisplaySeedPhraseWarning = remember(state.seedPhraseState) {
                    state.seedPhraseState.shouldDisplayInvalidSeedPhraseWarning()
                }

                if (shouldDisplaySeedPhraseWarning) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    WarningText(
                        modifier = Modifier.fillMaxWidth(),
                        text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure))
                    )
                }
            }
        }
    }
}

@UsesSampleValues
@Preview
@Composable
private fun ImportMnemonicsContentPreview(
    @PreviewParameter(ImportMnemonicsPreviewProvider::class) state: ImportMnemonicsViewModel.State,
) {
    RadixWalletTheme {
        ImportMnemonicsContent(
            state = state,
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onContinueClick = {},
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onWordSelected = { _, _ -> },
            onMessageShown = {}
        )
    }
}

@UsesSampleValues
class ImportMnemonicsPreviewProvider : PreviewParameterProvider<ImportMnemonicsViewModel.State> {

    override val values: Sequence<ImportMnemonicsViewModel.State>
        get() = sequenceOf(
            ImportMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        factorSource = FactorSource.Device.sample(),
                        card = FactorSource.Device.sample().toFactorSourceCard()
                    )
                ),
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = (0 until 24).map {
                        SeedPhraseWord(
                            index = it,
                            lastWord = it == 23
                        )
                    }.toPersistentList()
                ),
                isLoading = false,
                selectedIndex = 0
            ),
            ImportMnemonicsViewModel.State()
        )
}
