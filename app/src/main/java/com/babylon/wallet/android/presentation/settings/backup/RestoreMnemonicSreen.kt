package com.babylon.wallet.android.presentation.settings.backup

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.Scaffold
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
import com.babylon.wallet.android.presentation.settings.legacyimport.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList

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
    seedPhraseWords: ImmutableList<SeedPhraseWord>,
    bip39Passphrase: String,
    wordAutocompleteCandidates: ImmutableList<String>
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val kbVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val stripHeight by animateDpAsState(
        targetValue = if (wordAutocompleteCandidates.isNotEmpty() && kbVisible) {
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

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.importMnemonic_tempAndroid_heading),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            RadixPrimaryButton(
                text = stringResource(R.string.common_continue),
                onClick = onRestore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = RadixTheme.dimensions.paddingDefault),
                enabled = state.seedPhraseValid,
                throttleClicks = true
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        ) {
            var isSeedPhraseMenuExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = stripHeight)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
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
            }
            if (wordAutocompleteCandidates.isNotEmpty() && kbVisible) {
                SeedPhraseSuggestions(
                    wordAutocompleteCandidates = wordAutocompleteCandidates,
                    modifier = Modifier
                        .imePadding()
                        .align(Alignment.BottomCenter)
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
            }
        }
    }
}
