package com.babylon.wallet.android.presentation.onboarding.restore.mnemonic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.onboarding.restore.common.views.ImportMnemonicContentView
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ImportSingleMnemonicScreen(
    viewModel: ImportSingleMnemonicViewModel,
    onBackClick: () -> Unit,
    onStartRecovery: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    ImportSingleMnemonicsContent(
        state = state,
        onBackClick = onBackClick,
        onContinueClick = viewModel::onSubmitClick,
        onWordChanged = viewModel::onWordChanged,
        onWordSelected = viewModel::onWordSelected,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ImportSingleMnemonicViewModel.Event.FactorSourceAdded -> onBackClick()
                ImportSingleMnemonicViewModel.Event.MainSeedPhraseCompleted -> onStartRecovery()
            }
        }
    }
}

@Composable
private fun ImportSingleMnemonicsContent(
    modifier: Modifier = Modifier,
    state: ImportSingleMnemonicViewModel.State,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onMessageShown: () -> Unit
) {
    ImportMnemonicContentView(
        modifier = modifier,
        isLoading = state.isLoading,
        uiMessage = state.uiMessage,
        factorSourceCard = state.factorSourceCard,
        isOlympia = state.isOlympia,
        seedPhraseState = state.seedPhraseState,
        bottomBar = {
            RadixBottomBar(
                text = stringResource(R.string.common_confirm),
                enabled = state.isButtonEnabled,
                isLoading = state.isButtonLoading,
                onClick = onContinueClick
            )
        },
        onBackClick = onBackClick,
        onWordChanged = onWordChanged,
        onWordSelected = onWordSelected,
        onPassphraseChanged = onPassphraseChanged,
        onMessageShown = onMessageShown
    )
}

@UsesSampleValues
@Preview
@Composable
fun ImportSingleMnemonicPreview() {
    RadixWalletTheme {
        ImportSingleMnemonicsContent(
            state = ImportSingleMnemonicViewModel.State(
                isLoading = false,
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = (0 until 24).map {
                        SeedPhraseWord(
                            index = it,
                            lastWord = it == 23
                        )
                    }.toPersistentList()
                ),
                factorSourceCard = FactorSource.sample().toFactorSourceCard()
            ),
            onBackClick = {},
            onContinueClick = {},
            onWordChanged = { _, _ -> },
            onWordSelected = { _, _ -> },
            onPassphraseChanged = {},
            onMessageShown = {}
        )
    }
}
