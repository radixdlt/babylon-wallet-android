package com.babylon.wallet.android.presentation.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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

@Composable
fun RestoreMnemonicScreen(
    modifier: Modifier = Modifier,
    viewModel: RestoreMnemonicViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    RestoreMnemonicContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        onMnemonicWordsTyped = viewModel::onMnemonicWordsTyped,
        onPassphraseTyped = viewModel::onPassphraseTyped,
        onRestore = viewModel::onRestore
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
                text = stringResource(R.string.continue_button_title),
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            val focusManager = LocalFocusManager.current

            Text(
                text = state.factorSourceHint,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )

            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onMnemonicWordsTyped,
                value = state.mnemonicWords,
                leftLabel = stringResource(id = R.string.seed_phrase),
                hint = stringResource(id = R.string.seed_phrase),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onPassphraseTyped,
                value = state.passphrase,
                leftLabel = stringResource(id = R.string.bip_39_passphrase),
                hint = stringResource(id = R.string.passphrase),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}
