package com.babylon.wallet.android.presentation.settings.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun RestoreMnemonicScreen(
    modifier: Modifier = Modifier,
    viewModel: RestoreMnemonicViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    RestoreMnemonicContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        onMnemonicWordsTyped = viewModel::onMnemonicWordsTyped,
        onChangeSeedPhraseLength = viewModel::onChangeSeedPhraseLength,
        onPassphraseTyped = viewModel::onPassphraseTyped,
        onRestore = {
            context.biometricAuthenticate { authenticated ->
                if (authenticated) {
                    viewModel.onRestore()
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is RestoreMnemonicViewModel.Effect.FinishRestoration -> onBackClick()
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
    onMnemonicWordsTyped: (String) -> Unit,
    onChangeSeedPhraseLength: (SeedPhraseLength) -> Unit,
    onPassphraseTyped: (String) -> Unit,
    onRestore: () -> Unit
) {
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
                title = stringResource(id = R.string.recover_mnemonic),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.common_continue),
                onClick = onRestore,
                enabled = state.isSubmitButtonEnabled,
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
        var isSeedPhraseMenuExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        ) {
            val focusManager = LocalFocusManager.current

            Text(
                text = state.factorSourceLabel,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingDefault))

            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onMnemonicWordsTyped,
                value = state.wordsPhrase,
                leftLabel = stringResource(id = R.string.importOlympiaAccounts_seedPhrase),
                hint = stringResource(id = R.string.importOlympiaAccounts_seedPhrase),
                rightLabel = state.wordsHint,
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            Row {
                Spacer(modifier = Modifier.weight(1f))

                Box {
                    UnderlineTextButton(
                        text = stringResource(R.string.change_seed_phrase_length),
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
                                        text = stringResource(id = R.string.word_seed_phrase, seedPhraseLength.words),
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

            Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingDefault))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onPassphraseTyped,
                value = state.passphrase,
                leftLabel = stringResource(id = R.string.importOlympiaAccounts_bip39passphrase),
                hint = stringResource(id = R.string.importOlympiaAccounts_passphrase),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}
