package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun ConfirmMnemonicScreen(
    viewModel: ConfirmMnemonicViewModel,
    onMnemonicBackedUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    ConfirmMnemonicContent(
        state = state,
        onBackClick = onDismiss,
        onSubmitClick = {
            context.biometricAuthenticate { authenticated ->
                if (authenticated) viewModel.onSubmit()
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
                ConfirmMnemonicViewModel.Event.MoveToNextWord -> focusManager.moveFocus(FocusDirection.Next)
                ConfirmMnemonicViewModel.Event.MnemonicBackedUp -> onMnemonicBackedUp()
            }
        }
    }
}

@Composable
private fun ConfirmMnemonicContent(
    modifier: Modifier = Modifier,
    state: ConfirmMnemonicViewModel.State,
    onBackClick: () -> Unit,
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
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(
                        id = R.string.common_continue
                    ),
                    enabled = state.seedPhraseState.seedPhraseValid,
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

        SeedPhraseView(
            modifier = Modifier.padding(padding),
            state = state,
            onWordChanged = onWordTyped,
            onPassphraseChanged = onPassphraseChanged,
            onFocusedWordIndexChanged = { focusedWordIndex = it }
        )
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    state: ConfirmMnemonicViewModel.State,
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
            text = "Confirm Your Seed Phrase", // TODO crowding
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = "Confirm you have written down the seed phrase by entering the missing words below.", // TODO crowdin
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header
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
            onFocusedWordIndexChanged = onFocusedWordIndexChanged,
            showAdvancedMode = false
        )
    }
}

@Composable
private fun isSuggestionsVisible(state: ConfirmMnemonicViewModel.State): Boolean {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val keyboardVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    return state.seedPhraseState.wordAutocompleteCandidates.isNotEmpty() && keyboardVisible
}

@Preview
@Composable
fun ConfirmMnemonicContentPreview() {
    RadixWalletTheme {
        ConfirmMnemonicContent(
            state = ConfirmMnemonicViewModel.State(
                factorSource = SampleDataProvider().babylonDeviceFactorSource()
            ),
            onBackClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {},
            onWordSelected = { _, _ -> }
        )
    }
}
