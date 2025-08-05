package com.babylon.wallet.android.presentation.onboarding.restore.common.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
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
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding

@Composable
fun ImportMnemonicContentView(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    uiMessage: UiMessage?,
    factorSourceCard: FactorSourceCard?,
    isOlympia: Boolean,
    seedPhraseState: SeedPhraseInputDelegate.State,
    bottomBar: @Composable () -> Unit,
    onBackClick: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onMessageShown: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    var focusedWordIndex by remember { mutableStateOf<Int?>(null) }
    val isSuggestionsVisible = seedPhraseState.rememberSuggestionsVisibilityState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
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
                    wordAutocompleteCandidates = seedPhraseState.wordAutocompleteCandidates,
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
            } else if (!isLoading) {
                bottomBar()
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

        if (!isLoading) {
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
                factorSourceCard?.let {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                    FactorSourceCardView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        item = it
                    )

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
                    onFocusedWordIndexChanged = { focusedWordIndex = it },
                    initiallyFocusedIndex = 0,
                    showAdvancedMode = isOlympia
                )

                val shouldDisplaySeedPhraseWarning = remember(seedPhraseState) {
                    seedPhraseState.shouldDisplayInvalidSeedPhraseWarning()
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
