package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsViewModel.Event
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun RestoreMnemonicsScreen(
    viewModel: RestoreMnemonicsViewModel,
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    RestoreMnemonicsContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSkipSeedPhraseClick = {
            viewModel.onSkipSeedPhraseClick {
                context.biometricAuthenticateSuspend()
            }
        },
        onSkipMainSeedPhraseClick = viewModel::onSkipMainSeedPhraseClick,
        onSubmitClick = {
            when (state.screenType) {
                RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase -> {
                    viewModel.skipMainSeedPhraseAndCreateNew {
                        context.biometricAuthenticateSuspend()
                    }
                }
                RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase -> {
                    context.biometricAuthenticate { authenticated ->
                        if (authenticated) {
                            viewModel.onSubmit()
                        }
                    }
                }
                else -> {
                    viewModel.onSubmit()
                }
            }
        },
        onWordTyped = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onMessageShown = viewModel::onMessageShown,
        onWordSelected = viewModel::onWordSelected
    )

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.FinishRestoration -> onDismiss(it.isMovingToMain)
                is Event.MoveToNextWord -> focusManager.moveFocus(FocusDirection.Next)
                is Event.CloseApp -> onCloseApp()
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun RestoreMnemonicsContent(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State,
    onBackClick: () -> Unit,
    onSkipSeedPhraseClick: () -> Unit,
    onSkipMainSeedPhraseClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onWordTyped: (Int, String) -> Unit,
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
            if (state.screenType != RestoreMnemonicsViewModel.State.ScreenType.Entities &&
                isSuggestionsVisible(state = state)
            ) {
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
                    if (state.screenType is RestoreMnemonicsViewModel.State.ScreenType.Entities) {
                        if (!state.isMainBabylonSeedPhrase) {
                            RadixTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = RadixTheme.dimensions.paddingMedium,
                                        start = RadixTheme.dimensions.paddingLarge,
                                        end = RadixTheme.dimensions.paddingLarge,
                                    ),
                                text = stringResource(id = R.string.recoverSeedPhrase_skipButton),
                                onClick = onSkipSeedPhraseClick
                            )
                        }

                        if (state.isMainBabylonSeedPhrase && state.isMandatory.not()) {
                            RadixTextButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = RadixTheme.dimensions.paddingMedium,
                                        start = RadixTheme.dimensions.paddingLarge,
                                        end = RadixTheme.dimensions.paddingLarge,
                                    ),
                                text = stringResource(id = R.string.recoverSeedPhrase_noMainSeedPhraseButton),
                                onClick = onSkipMainSeedPhraseClick
                            )
                        }
                    }

                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(
                            when (state.screenType) {
                                RestoreMnemonicsViewModel.State.ScreenType.Entities -> R.string.recoverSeedPhrase_enterButton
                                RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase -> R.string.common_continue
                                RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase ->
                                    R.string.recoverSeedPhrase_skipMainSeedPhraseButton
                            }
                        ),
                        enabled = state.screenType != RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase ||
                            state.seedPhraseState.seedPhraseValid,
                        isLoading = state.isRestoring,
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
        when (state.screenType) {
            RestoreMnemonicsViewModel.State.ScreenType.Entities -> {
                AnimatedVisibility(
                    modifier = Modifier.padding(padding),
                    visible = true,
                    enter = slideInHorizontally(initialOffsetX = { if (state.isMovingForward) it else -it }),
                    exit = slideOutHorizontally(targetOffsetX = { if (state.isMovingForward) it else -it })
                ) {
                    EntitiesView(
                        state = state
                    )
                }
            }
            RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase -> {
                AnimatedVisibility(
                    modifier = Modifier.padding(padding),
                    visible = true,
                    enter = slideInHorizontally(initialOffsetX = { if (state.isMovingForward) -it else it }),
                    exit = slideOutHorizontally(targetOffsetX = { if (state.isMovingForward) -it else it })
                ) {
                    SeedPhraseView(
                        state = state,
                        onWordChanged = onWordTyped,
                        onPassphraseChanged = onPassphraseChanged,
                        onFocusedWordIndexChanged = { focusedWordIndex = it }
                    )
                }
            }
            RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase -> {
                AnimatedVisibility(
                    modifier = Modifier.padding(padding),
                    visible = true,
                    enter = slideInHorizontally(initialOffsetX = { if (state.isMovingForward) -it else it }),
                    exit = slideOutHorizontally(targetOffsetX = { if (state.isMovingForward) -it else it })
                ) {
                    NoMainSeedPhraseView()
                }
            }
        }
    }
}

@Composable
private fun EntitiesView(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(
                id = if (state.isMainBabylonSeedPhrase) {
                    R.string.recoverSeedPhrase_header_titleMain
                } else {
                    R.string.recoverSeedPhrase_header_titleOther
                }
            ),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(
                id = if (state.isMainBabylonSeedPhrase) {
                    R.string.recoverSeedPhrase_header_subtitleMainSeedPhrase
                } else {
                    R.string.recoverSeedPhrase_header_subtitleOtherSeedPhrase
                }
            ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        state.recoverableFactorSource?.let { recoverable ->
            if (recoverable.allAccountsHidden) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge)
                        .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectMedium)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingXLarge),
                        text = stringResource(id = R.string.recoverSeedPhrase_hidden_accounts_only),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
                ) {
                    items(recoverable.nonHiddenAccountsToDisplay) { account ->
                        SimpleAccountCard(
                            modifier = Modifier.fillMaxWidth(),
                            account = account
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMainSeedPhraseView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_header_titleNoMainSeedPhrase),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_header_subtitleNoMainSeedPhrase)
                .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
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
            text = stringResource(id = R.string.enterSeedPhrase_header_title),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        InfoLink(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.enterSeedPhrase_warning),
            contentColor = RadixTheme.colors.orange1,
            iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        SeedPhraseInputForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            seedPhraseWords = state.seedPhraseState.seedPhraseWords,
            bip39Passphrase = state.seedPhraseState.bip39Passphrase,
            onWordChanged = onWordChanged,
            onPassphraseChanged = onPassphraseChanged,
            onFocusedWordIndexChanged = onFocusedWordIndexChanged
        )
    }
}

@Composable
private fun isSuggestionsVisible(state: RestoreMnemonicsViewModel.State): Boolean {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val keyboardVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    return state.seedPhraseState.wordAutocompleteCandidates.isNotEmpty() && keyboardVisible
}

@Preview
@Composable
fun RestoreMnemonicsIntroContent() {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = List(5) { index ->
                            SampleDataProvider().sampleAccount(
                                address = "rdx_abcdefg$index",
                                name = "Account $index",
                                appearanceId = index
                            )
                        },
                        factorSource = SampleDataProvider().babylonDeviceFactorSource()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.Entities
            ),
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onSkipMainSeedPhraseClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {},
            onWordSelected = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun RestoreMnemonicsSeedPhraseContent() {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = List(5) { index ->
                            SampleDataProvider().sampleAccount(
                                address = "rdx_abcdefg$index",
                                name = "Account $index",
                                appearanceId = index
                            )
                        },
                        factorSource = SampleDataProvider().babylonDeviceFactorSource()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.Entities
            ),
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onSkipMainSeedPhraseClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {},
            onWordSelected = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun RestoreMnemonicsNoMainSeedPhraseContent() {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = List(5) { index ->
                            SampleDataProvider().sampleAccount(
                                address = "rdx_abcdefg$index",
                                name = "Account $index",
                                appearanceId = index
                            )
                        },
                        factorSource = SampleDataProvider().babylonDeviceFactorSource()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase
            ),
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onSkipMainSeedPhraseClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {},
            onWordSelected = { _, _ -> }
        )
    }
}
