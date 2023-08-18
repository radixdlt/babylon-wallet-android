package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun RestoreMnemonicsScreen(
    viewModel: RestoreMnemonicsViewModel,
    onFinish: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    RestoreMnemonicsContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSkipClicked = viewModel::onSkipClick,
        onSubmitClick = {
            if (!state.isShowingEntities) {
                context.biometricAuthenticate { authenticated ->
                    if (authenticated) {
                        viewModel.onSubmit()
                    }
                }
            } else {
                viewModel.onSubmit()
            }
        },
        onWordChanged = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onMessageShown = viewModel::onMessageShown
    )

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.FinishRestoration -> onFinish(it.isMovingToMain)
                is Event.MoveToNextWord -> focusManager.moveFocus(FocusDirection.Next)
            }
        }
    }
}

@Composable
private fun RestoreMnemonicsContent(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State,
    onBackClick: () -> Unit,
    onSkipClicked: () -> Unit,
    onSubmitClick: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
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
            RadixCenteredTopAppBar(title = "", onBackClick = onBackClick)
        },
        bottomBar = {
            if (!state.isShowingEntities && isSuggestionsVisible(state = state)) {
                SeedPhraseSuggestions(
                    wordAutocompleteCandidates = state.seedPhraseState.wordAutocompleteCandidates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .height(56.dp)
                        .padding(RadixTheme.dimensions.paddingSmall),
                    onCandidateClick = { candidate ->
                        focusedWordIndex?.let {
                            onWordChanged(it, candidate)
                            focusedWordIndex = null
                        }
                    }
                )
            } else {
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(
                        id = if (state.isShowingEntities) {
                            R.string.recoverSeedPhrase_enterButton
                        } else {
                            R.string.common_continue
                        }
                    ),
                    enabled = state.isShowingEntities || state.seedPhraseState.seedPhraseValid,
                    isLoading = state.isRestoring,
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
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        AnimatedVisibility(
            modifier = Modifier.padding(padding),
            visible = state.isShowingEntities,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            EntitiesView(
                state = state,
                onSkipClicked = onSkipClicked
            )
        }

        AnimatedVisibility(
            modifier = Modifier.padding(padding),
            visible = !state.isShowingEntities,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SeedPhraseView(
                state = state,
                onWordChanged = onWordChanged,
                onPassphraseChanged = onPassphraseChanged,
                onFocusedWordIndexChanged = { focusedWordIndex = it }
            )
        }
    }
}

@Composable
private fun EntitiesView(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State,
    onSkipClicked: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_header_title),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(
                id = if (state.recoverableFactorSource?.associatedPersonas?.isNotEmpty() == true) {
                    R.string.recoverSeedPhrase_header_subtitleAccountsPersonas
                } else {
                    R.string.recoverSeedPhrase_header_subtitleAccounts
                }
            ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        RadixTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_skipButton),
            onClick = onSkipClicked
        )

        state.recoverableFactorSource?.let { recoverable ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
            ) {
                items(recoverable.associatedAccounts) { account ->
                    SimpleAccountCard(
                        modifier = Modifier.fillMaxWidth(),
                        account = account
                    )
                }

                items(recoverable.associatedPersonas) { persona ->
                    StandardOneLineCard(
                        "",
                        persona.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.gray5,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            ),
                        showChevron = false
                    )
                }
            }
        }
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
private fun RestoreMnemonicsIntroContent() {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = List(5) { index ->
                            SampleDataProvider().sampleAccount(address = "rdx_abcdefg$index", name = "Account $index", appearanceId = index)
                        },
                        associatedPersonas = List(2) { index ->
                            SampleDataProvider().samplePersona(personaAddress = "rdx_abcdefg$index", "Persona $index")
                        },
                        factorSource = SampleDataProvider().deviceFactorSource()
                    )
                ),
                isShowingEntities = true
            ),
            onBackClick = {},
            onSkipClicked = {},
            onSubmitClick = {},
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
private fun RestoreMnemonicsSeedPhraseContent() {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = List(5) { index ->
                            SampleDataProvider().sampleAccount(address = "rdx_abcdefg$index", name = "Account $index", appearanceId = index)
                        },
                        associatedPersonas = List(2) { index ->
                            SampleDataProvider().samplePersona(personaAddress = "rdx_abcdefg$index", "Persona $index")
                        },
                        factorSource = SampleDataProvider().deviceFactorSource()
                    )
                ),
                isShowingEntities = false
            ),
            onBackClick = {},
            onSkipClicked = {},
            onSubmitClick = {},
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {}
        )
    }
}
