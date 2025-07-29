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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.rememberSuggestionsVisibilityState
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.HideKeyboardOnFullScroll
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.sargon.sample

@Composable
fun RestoreMnemonicsScreen(
    viewModel: RestoreMnemonicsViewModel,
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.biometricAuthProvider = { context.biometricAuthenticateSuspend() }
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    RestoreMnemonicsContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSkipSeedPhraseClick = {
            viewModel.onSkipSeedPhraseClick()
        },
        onSkipMainSeedPhraseClick = viewModel::onSkipMainSeedPhraseClick,
        onSubmitClick = {
            when (state.screenType) {
                RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase -> {
                    viewModel.skipMainSeedPhraseAndCreateNew()
                }

                else -> {
                    keyboardController?.hide()
                    viewModel.onSubmit()
                }
            }
        },
        onWordTyped = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onWordSelected = viewModel::onWordSelected,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is RestoreMnemonicsViewModel.Event.FinishRestoration -> onDismiss(it.isMovingToMain)
                is RestoreMnemonicsViewModel.Event.CloseApp -> onCloseApp()
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
            if (state.screenType != RestoreMnemonicsViewModel.State.ScreenType.Entities && isSuggestionsVisible) {
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
            } else if (state.screenType != RestoreMnemonicsViewModel.State.ScreenType.Loading) {
                RadixBottomBar(
                    text = stringResource(
                        when (state.screenType) {
                            RestoreMnemonicsViewModel.State.ScreenType.Loading -> R.string.empty
                            RestoreMnemonicsViewModel.State.ScreenType.Entities -> R.string.recoverSeedPhrase_enterButton
                            RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase -> R.string.common_continue
                            RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase ->
                                R.string.recoverSeedPhrase_skipMainSeedPhraseButton
                        }
                    ),
                    enabled = state.isPrimaryButtonEnabled,
                    isLoading = state.isPrimaryButtonLoading,
                    onClick = onSubmitClick,
                    additionalTopContent = {
                        if (state.screenType is RestoreMnemonicsViewModel.State.ScreenType.Entities) {
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
        containerColor = RadixTheme.colors.background
    ) { padding ->
        if (state.screenType !is RestoreMnemonicsViewModel.State.ScreenType.Loading) {
            val enterTransition = slideInHorizontally(initialOffsetX = { if (state.isMovingForward) it else -it })
            val exitTransition = slideOutHorizontally(targetOffsetX = { if (state.isMovingForward) -it else it })

            AnimatedVisibility(
                modifier = Modifier.padding(padding),
                visible = state.screenType is RestoreMnemonicsViewModel.State.ScreenType.Entities,
                enter = enterTransition,
                exit = exitTransition
            ) {
                EntitiesView(
                    state = state
                )
            }

            AnimatedVisibility(
                modifier = Modifier.keyboardVisiblePadding(
                    padding = padding,
                    bottom = if (isSuggestionsVisible) {
                        RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight
                    } else {
                        RadixTheme.dimensions.paddingDefault
                    }
                ),
                visible = state.screenType is RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase,
                enter = enterTransition,
                exit = exitTransition
            ) {
                SeedPhraseView(
                    state = state,
                    onWordChanged = onWordTyped,
                    onPassphraseChanged = onPassphraseChanged,
                    onFocusedWordIndexChanged = { focusedWordIndex = it }
                )
            }

            AnimatedVisibility(
                modifier = Modifier.padding(padding),
                visible = state.screenType is RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase,
                enter = enterTransition,
                exit = exitTransition
            ) {
                NoMainSeedPhraseView()
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
            text = stringResource(id = R.string.recoverSeedPhrase_header_titleOther),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_header_subtitleOtherSeedPhrase)
                .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        state.recoverableFactorSource?.let { recoverable ->
            if (recoverable.areAllAccountsHidden) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge)
                        .background(RadixTheme.colors.backgroundSecondary, shape = RadixTheme.shapes.roundedRectMedium)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingXXLarge),
                        text = stringResource(id = R.string.recoverSeedPhrase_hiddenAccountsOnly),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
                ) {
                    items(recoverable.activeAccountsToDisplay) { account ->
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
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverSeedPhrase_header_subtitleNoMainSeedPhrase)
                .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    state: RestoreMnemonicsViewModel.State,
    onWordChanged: (Int, String) -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
    onPassphraseChanged: (String) -> Unit
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
            text = stringResource(id = R.string.enterSeedPhrase_header_title),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        SeedPhraseInputForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            seedPhraseWords = state.seedPhraseState.seedPhraseWords,
            bip39Passphrase = state.seedPhraseState.bip39Passphrase,
            onWordChanged = onWordChanged,
            onPassphraseChanged = onPassphraseChanged,
            onFocusedWordIndexChanged = onFocusedWordIndexChanged,
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

@UsesSampleValues
@Preview
@Composable
private fun RestoreMnemonicsContentPreview(
    @PreviewParameter(RestoreMnemonicsPreviewProvider::class) state: RestoreMnemonicsViewModel.State,
) {
    RadixWalletTheme {
        RestoreMnemonicsContent(
            state = state,
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onSkipMainSeedPhraseClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onPassphraseChanged = {},
            onWordSelected = { _, _ -> },
            onMessageShown = {}
        )
    }
}

@UsesSampleValues
class RestoreMnemonicsPreviewProvider : PreviewParameterProvider<RestoreMnemonicsViewModel.State> {

    override val values: Sequence<RestoreMnemonicsViewModel.State>
        get() = sequenceOf(
            RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = Account.sampleMainnet.all,
                        factorSource = FactorSource.Device.sample()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.Entities
            ),
            RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = Account.sampleMainnet.all,
                        factorSource = FactorSource.Device.sample()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.SeedPhrase,
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = (0 until 24).map {
                        SeedPhraseWord(
                            index = it,
                            lastWord = it == 23
                        )
                    }.toPersistentList()
                )
            ),
            RestoreMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        associatedAccounts = Account.sampleMainnet.all,
                        factorSource = FactorSource.Device.sample()
                    )
                ),
                screenType = RestoreMnemonicsViewModel.State.ScreenType.NoMainSeedPhrase
            ),
            RestoreMnemonicsViewModel.State(
                screenType = RestoreMnemonicsViewModel.State.ScreenType.Loading
            )
        )
}
