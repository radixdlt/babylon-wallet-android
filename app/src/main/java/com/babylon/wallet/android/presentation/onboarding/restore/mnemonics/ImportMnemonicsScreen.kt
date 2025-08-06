package com.babylon.wallet.android.presentation.onboarding.restore.mnemonics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.onboarding.restore.common.views.ImportMnemonicContentView
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsViewModel.RecoverableFactorSource
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.sargon.sample

@Composable
fun ImportMnemonicsScreen(
    viewModel: ImportMnemonicsViewModel,
    onCloseApp: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    ImportMnemonicsContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSkipSeedPhraseClick = {
            viewModel.onSkipSeedPhraseClick()
        },
        onContinueClick = {
            keyboardController?.hide()
            viewModel.onContinueClick()
        },
        onWordChanged = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onWordSelected = viewModel::onWordSelected,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is ImportMnemonicsViewModel.Event.FinishRestoration -> onDismiss(it.isMovingToMain)
                is ImportMnemonicsViewModel.Event.CloseApp -> onCloseApp()
            }
        }
    }
}

@Composable
private fun ImportMnemonicsContent(
    modifier: Modifier = Modifier,
    state: ImportMnemonicsViewModel.State,
    onBackClick: () -> Unit,
    onSkipSeedPhraseClick: () -> Unit,
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
        factorSourceCard = state.recoverableFactorSource?.card,
        isOlympia = state.isOlympia,
        seedPhraseState = state.seedPhraseState,
        bottomBar = {
            RadixBottomBar(
                text = stringResource(R.string.common_continue),
                enabled = state.isPrimaryButtonEnabled,
                isLoading = state.isPrimaryButtonLoading,
                onClick = onContinueClick,
                additionalTopContent = {
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
private fun ImportMnemonicsContentPreview(
    @PreviewParameter(ImportMnemonicsPreviewProvider::class) state: ImportMnemonicsViewModel.State,
) {
    RadixWalletPreviewTheme {
        ImportMnemonicsContent(
            state = state,
            onBackClick = {},
            onSkipSeedPhraseClick = {},
            onContinueClick = {},
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onWordSelected = { _, _ -> },
            onMessageShown = {}
        )
    }
}

@UsesSampleValues
class ImportMnemonicsPreviewProvider : PreviewParameterProvider<ImportMnemonicsViewModel.State> {

    override val values: Sequence<ImportMnemonicsViewModel.State>
        get() = sequenceOf(
            ImportMnemonicsViewModel.State(
                recoverableFactorSources = listOf(
                    RecoverableFactorSource(
                        factorSource = FactorSource.Device.sample(),
                        card = FactorSource.Device.sample().toFactorSourceCard()
                    )
                ),
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = (0 until 24).map {
                        SeedPhraseWord(
                            index = it,
                            lastWord = it == 23
                        )
                    }.toPersistentList()
                ),
                isLoading = false,
                selectedIndex = 0
            ),
            ImportMnemonicsViewModel.State()
        )
}
