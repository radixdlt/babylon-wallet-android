package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.forgotpin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.addfactorsource.seedphrase.SeedPhraseScreen
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ForgotArculusPinScreen(
    modifier: Modifier = Modifier,
    viewModel: ForgotArculusPinViewModel,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ForgotArculusPinContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onDismissMessage = viewModel::onDismissMessage,
        onWordChanged = viewModel::onWordChanged,
        onWordSelected = viewModel::onWordSelected,
        onNumberOfWordsChanged = viewModel::onNumberOfWordsChanged,
        onConfirmClick = viewModel::onConfirmClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ForgotArculusPinViewModel.Event.Complete -> onComplete()
                ForgotArculusPinViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }
}

@Composable
private fun ForgotArculusPinContent(
    modifier: Modifier = Modifier,
    state: ForgotArculusPinViewModel.State,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onNumberOfWordsChanged: (Bip39WordCount) -> Unit,
    onConfirmClick: () -> Unit
) {
    var focusedWordIndex by remember { mutableStateOf<Int?>(null) }

    SeedPhraseScreen(
        modifier = modifier,
        state = state.seedPhraseState,
        title = stringResource(id = R.string.enterSeedPhrase_header_title),
        description = "Enter your Arculus seed phrase", // TODO crowdin
        isConfirmButtonEnabled = state.isConfirmButtonEnabled,
        focusedWordIndex = focusedWordIndex,
        numberOfWordsOptions = persistentListOf(
            Bip39WordCount.TWENTY_FOUR,
            Bip39WordCount.TWELVE
        ),
        showNumberOfWordsPicker = true,
        showAdvancedMode = false,
        isEditingEnabled = true,
        errorMessage = state.errorMessage,
        onDismiss = onDismiss,
        onDismissMessage = onDismissMessage,
        onFocusedWordIndexChanged = { focusedWordIndex = it },
        onWordChanged = onWordChanged,
        onWordSelected = onWordSelected,
        onNumberOfWordsChanged = onNumberOfWordsChanged,
        onConfirmClick = onConfirmClick
    )
}

@UsesSampleValues
@Composable
@Preview
private fun ForgotArculusPinPreview() {
    RadixWalletPreviewTheme {
        ForgotArculusPinContent(
            state = ForgotArculusPinViewModel.State(
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = Mnemonic.sample().words.mapIndexed { index, bip39Word ->
                        SeedPhraseWord(
                            index = index,
                            value = bip39Word.word,
                            state = SeedPhraseWord.State.ValidDisabled
                        )
                    }.toPersistentList()
                )
            ),
            onDismiss = {},
            onDismissMessage = {},
            onWordChanged = { _, _ -> },
            onWordSelected = { _, _ -> },
            onNumberOfWordsChanged = {},
            onConfirmClick = {}
        )
    }
}
