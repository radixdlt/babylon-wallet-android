package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.rememberSuggestionsVisibilityState
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.HideKeyboardOnFullScroll
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.Bip39WordCount

@Composable
fun AddSingleMnemonicScreen(
    viewModel: AddSingleMnemonicViewModel,
    onBackClick: () -> Unit,
    onStartRecovery: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    AddSingleMnemonicsContent(
        state = state,
        onBackClick = onBackClick,
        onSubmitClick = {
            if (state.mnemonicType == MnemonicType.BabylonMain) { // screen opened from onboarding flow
                viewModel.onAddMainSeedPhrase()
            } else {
                context.biometricAuthenticate { result ->
                    if (result == BiometricAuthenticationResult.Succeeded) {
                        viewModel.onAddFactorSource()
                    }
                }
            }
        },
        onWordTyped = viewModel::onWordChanged,
        onWordSelected = viewModel::onWordSelected,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onMessageShown = viewModel::onMessageShown,
        onSeedPhraseLengthChanged = viewModel::onSeedPhraseLengthChanged
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                AddSingleMnemonicViewModel.Event.FactorSourceAdded -> onBackClick()
                AddSingleMnemonicViewModel.Event.MainSeedPhraseCompleted -> onStartRecovery()
            }
        }
    }
}

@Composable
private fun AddSingleMnemonicsContent(
    modifier: Modifier = Modifier,
    state: AddSingleMnemonicViewModel.State,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onWordTyped: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onMessageShown: () -> Unit,
    onSeedPhraseLengthChanged: (Bip39WordCount) -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }

    val isSuggestionsVisible = state.seedPhraseState.rememberSuggestionsVisibilityState()

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
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
            } else {
                RadixBottomBar(
                    text = stringResource(R.string.common_continue),
                    enabled = remember(state.seedPhraseState) {
                        state.seedPhraseState.isValidSeedPhrase()
                    },
                    onClick = onSubmitClick
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        val title = when (state.mnemonicType) {
            MnemonicType.BabylonMain -> stringResource(id = R.string.enterSeedPhrase_titleBabylonMain)
            MnemonicType.Babylon -> stringResource(id = R.string.enterSeedPhrase_titleBabylon)
            MnemonicType.Olympia -> stringResource(id = R.string.enterSeedPhrase_titleOlympia)
        }
        val isOlympia = state.mnemonicType == MnemonicType.Olympia
        Box(
            modifier = Modifier
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = if (isSuggestionsVisible) {
                        RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight
                    } else {
                        RadixTheme.dimensions.paddingDefault
                    }
                )
        ) {
            SeedPhraseView(
                title = title,
                onWordChanged = onWordTyped,
                onPassphraseChanged = onPassphraseChanged,
                onFocusedWordIndexChanged = { focusedWordIndex = it },
                seedPhraseState = state.seedPhraseState,
                onSeedPhraseLengthChanged = onSeedPhraseLengthChanged,
                isOlympia = isOlympia
            )
        }
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    title: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
    onSeedPhraseLengthChanged: (Bip39WordCount) -> Unit = {},
    seedPhraseState: SeedPhraseInputDelegate.State,
    isOlympia: Boolean
) {
    SecureScreen()

    val scrollState = rememberScrollState()
    HideKeyboardOnFullScroll(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = title,
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        WarningText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = AnnotatedString(stringResource(R.string.enterSeedPhrase_warning)),
            iconRes = DSR.ic_warning_error
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (isOlympia) {
            val tabs = remember {
                Bip39WordCount.entries.sortedBy { it.value }
            }
            var tabIndex by remember { mutableStateOf(0) }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.importMnemonic_numberOfWordsPicker),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            TabRow(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .background(RadixTheme.colors.backgroundTertiary, RadixTheme.shapes.roundedRectSmall),
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[tabIndex])
                            .fillMaxHeight()
                            .zIndex(-1f)
                            .padding(2.dp)
                            .background(
                                color = RadixTheme.colors.selectedSegmentedControl,
                                shape = RadixTheme.shapes.roundedRectSmall
                            )
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val isSelected = tabs.indexOf(tab) == tabIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    Tab(
                        modifier = Modifier.wrapContentWidth(),
                        selected = isSelected,
                        onClick = {
                            tabIndex = tabs.indexOf(tab)
                            onSeedPhraseLengthChanged(tab)
                        },
                        interactionSource = interactionSource,
                        selectedContentColor = RadixTheme.colors.text,
                        unselectedContentColor = RadixTheme.colors.textSecondary
                    ) {
                        Text(
                            modifier = Modifier.padding(
                                vertical = RadixTheme.dimensions.paddingSmall
                            ),
                            text = tab.value.toString(),
                            style = RadixTheme.typography.body1HighImportance,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        SeedPhraseInputForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .padding(bottom = RadixTheme.dimensions.paddingXLarge),
            seedPhraseWords = seedPhraseState.seedPhraseWords,
            bip39Passphrase = seedPhraseState.bip39Passphrase,
            onWordChanged = onWordChanged,
            onPassphraseChanged = onPassphraseChanged,
            onFocusedWordIndexChanged = onFocusedWordIndexChanged,
            showAdvancedMode = isOlympia,
            initiallyFocusedIndex = 0
        )

        val shouldDisplayInvalidSeedPhraseWarning = remember(seedPhraseState) {
            seedPhraseState.shouldDisplayInvalidSeedPhraseWarning()
        }

        if (shouldDisplayInvalidSeedPhraseWarning) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            WarningText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure))
            )
        }
    }
}

@Preview
@Composable
fun AddMnemonicsSeedPhraseContent() {
    RadixWalletTheme {
        AddSingleMnemonicsContent(
            state = AddSingleMnemonicViewModel.State(),
            onBackClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onWordSelected = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {},
            onSeedPhraseLengthChanged = {}
        )
    }
}
