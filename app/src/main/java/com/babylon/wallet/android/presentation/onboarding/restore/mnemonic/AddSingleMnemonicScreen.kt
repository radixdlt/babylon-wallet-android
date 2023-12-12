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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.recover.AccountRecoveryViewModel
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RedWarningText
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate
import rdx.works.profile.data.model.SeedPhraseLength

@Composable
fun AddSingleMnemonicScreen(
    viewModel: AddSingleMnemonicViewModel,
    onBackClick: () -> Unit,
    sharedViewModel: AccountRecoveryViewModel? = null,
    onStartRecovery: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    AddSingleMnemonicsContent(
        state = state,
        onBackClick = onBackClick,
        onSubmitClick = {
            if (state.mnemonicType == MnemonicType.BabylonMain) {
                sharedViewModel?.initDeviceFactorSource(
                    mnemonicWithPassphrase = state.seedPhraseState.mnemonicWithPassphrase,
                    isMain = true
                )
                onStartRecovery()
            } else {
                context.biometricAuthenticate { authenticated ->
                    if (authenticated) {
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

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                AddSingleMnemonicViewModel.Event.MoveToNextWord -> focusManager.moveFocus(FocusDirection.Next)
                AddSingleMnemonicViewModel.Event.FactorSourceAdded -> onBackClick()
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
    onSeedPhraseLengthChanged: (Int) -> Unit
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

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            if (isSuggestionsVisible(state = state)) {
                SeedPhraseSuggestions(
                    wordAutocompleteCandidates = state.seedPhraseState.wordAutocompleteCandidates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .height(56.dp)
                        .padding(RadixTheme.dimensions.paddingSmall),
                    onCandidateClick = { candidate ->
                        focusedWordIndex?.let {
                            onWordSelected(it, candidate)
                            focusedWordIndex = null
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.common_continue),
                        enabled = state.seedPhraseState.seedPhraseInputValid && state.seedPhraseState.seedPhraseBIP39Valid,
                        onClick = onSubmitClick
                    )
                }
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        val title = when (state.mnemonicType) {
            MnemonicType.BabylonMain -> "Enter Main Seed Phrase"
            MnemonicType.Babylon -> "Enter Babylon Seed Phrase" // TODO crowdin
            MnemonicType.Olympia -> "Enter Legacy Seed Phrase" // TODO crowdin
        }
        val isOlympia = state.mnemonicType == MnemonicType.Olympia
        SeedPhraseView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    title: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
    onSeedPhraseLengthChanged: (Int) -> Unit = {},
    seedPhraseState: SeedPhraseInputDelegate.State,
    isOlympia: Boolean
) {
    SecureScreen()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = title,
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        InfoLink(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.enterSeedPhrase_warning),
            contentColor = RadixTheme.colors.orange1,
            iconRes = DSR.ic_warning_error
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (isOlympia) {
            val tabs = SeedPhraseLength.values()
            var tabIndex by remember { mutableStateOf(0) }
            TabRow(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectSmall),
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[tabIndex])
                            .fillMaxHeight()
                            .zIndex(-1f)
                            .background(RadixTheme.colors.white, RadixTheme.shapes.roundedRectSmall)
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
                            onSeedPhraseLengthChanged(tab.words)
                        },
                        interactionSource = interactionSource,
                        selectedContentColor = RadixTheme.colors.gray1,
                        unselectedContentColor = RadixTheme.colors.gray2
                    ) {
                        Text(
                            modifier = Modifier.padding(
                                vertical = RadixTheme.dimensions.paddingSmall
                            ),
                            text = tab.words.toString(),
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
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            seedPhraseWords = seedPhraseState.seedPhraseWords,
            bip39Passphrase = seedPhraseState.bip39Passphrase,
            onWordChanged = onWordChanged,
            onPassphraseChanged = onPassphraseChanged,
            onFocusedWordIndexChanged = onFocusedWordIndexChanged,
            showAdvancedMode = isOlympia
        )
        if (seedPhraseState.seedPhraseInputValid && seedPhraseState.seedPhraseBIP39Valid.not()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RedWarningText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure))
            )
        }
    }
}

@Composable
private fun isSuggestionsVisible(state: AddSingleMnemonicViewModel.State): Boolean {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val keyboardVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    return state.seedPhraseState.wordAutocompleteCandidates.isNotEmpty() && keyboardVisible
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
