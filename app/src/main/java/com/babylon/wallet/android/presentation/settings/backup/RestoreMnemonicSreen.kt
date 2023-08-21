package com.babylon.wallet.android.presentation.settings.backup

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import timber.log.Timber

@Composable
fun RestoreMnemonicScreen(
    modifier: Modifier = Modifier,
    viewModel: RestoreMnemonicViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    SecureScreen()
    RestoreMnemonicContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        onWordChanged = viewModel::onWordChanged,
        onChangeSeedPhraseLength = viewModel::onChangeSeedPhraseLength,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        seedPhraseWords = state.seedPhraseWords,
        onRestore = {
            context.biometricAuthenticate { authenticated ->
                if (authenticated) {
                    viewModel.onRestore()
                }
            }
        },
        bip39Passphrase = state.bip39Passphrase,
        wordAutocompleteCandidates = state.wordAutocompleteCandidates
    )

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is RestoreMnemonicViewModel.Effect.FinishRestoration -> onBackClick()
                RestoreMnemonicViewModel.Effect.MoveToNextWord -> {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            }
        }
    }
}

@Composable
private fun RestoreMnemonicContent(
    modifier: Modifier,
    state: RestoreMnemonicViewModel.State,
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onChangeSeedPhraseLength: (SeedPhraseLength) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onRestore: () -> Unit,
    seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord>,
    bip39Passphrase: String,
    wordAutocompleteCandidates: ImmutableList<String>
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val kbVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val isSeedPhraseSuggestionsVisible = wordAutocompleteCandidates.isNotEmpty() && kbVisible
    val stripHeight by animateDpAsState(
        targetValue = if (isSeedPhraseSuggestionsVisible) {
            56.dp
        } else {
            0.dp
        }
    )
    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Box(
        modifier
            .background(RadixTheme.colors.defaultBackground)
            .navigationBarsPadding()
    ) {
        var isSeedPhraseMenuExpanded by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.importMnemonic_tempAndroid_heading),
                onBackClick = onBackClick
            )
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = stripHeight)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            ) {
                Text(
                    text = state.factorSourceLabel,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingDefault))

                SeedPhraseInputForm(
                    seedPhraseWords = seedPhraseWords,
                    onWordChanged = onWordChanged,
                    onPassphraseChanged = onPassphraseChanged,
                    bip39Passphrase = bip39Passphrase,
                    modifier = Modifier.fillMaxWidth(),
                    onFocusedWordIndexChanged = {
                        focusedWordIndex = it
                    }
                )

                Row {
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        UnderlineTextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.importMnemonic_tempAndroid_changeSeedPhrase),
                            onClick = { isSeedPhraseMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isSeedPhraseMenuExpanded,
                            onDismissRequest = { isSeedPhraseMenuExpanded = false }
                        ) {
                            SeedPhraseLength.values().forEach { seedPhraseLength ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.importMnemonic_tempAndroid_seedLength,
                                                seedPhraseLength.words
                                            ),
                                            style = RadixTheme.typography.body1Regular,
                                            color = RadixTheme.colors.defaultText
                                        )
                                    },
                                    onClick = {
                                        isSeedPhraseMenuExpanded = false
                                        onChangeSeedPhraseLength(seedPhraseLength)
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = RadixTheme.dimensions.paddingDefault,
                                        vertical = RadixTheme.dimensions.paddingXSmall
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(height = 80.dp))
            }
        }
        if (isSeedPhraseSuggestionsVisible) {
            Timber.d("Autocomplete candidates: visible")
            SeedPhraseSuggestions(
                wordAutocompleteCandidates = wordAutocompleteCandidates,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault)
            ) {
                RadixPrimaryButton(
                    text = stringResource(R.string.common_continue),
                    onClick = onRestore,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    enabled = state.seedPhraseValid,
                    throttleClicks = true
                )
            }
        }
    }
}
