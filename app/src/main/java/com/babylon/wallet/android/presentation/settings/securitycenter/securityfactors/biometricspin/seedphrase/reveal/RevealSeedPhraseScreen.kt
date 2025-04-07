package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.reveal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.CopySeedPhraseButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.SeedPhraseRow
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.SeedPhraseSingleWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.FactorSourceId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RevealSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: RevealSeedPhraseViewModel,
    onBackClick: () -> Unit,
    onConfirmSeedPhraseClick: (FactorSourceId.Hash, Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backClickHandler = {
        if (state.backedUp) {
            onBackClick()
        } else {
            viewModel.showConfirmSeedPhraseDialog()
        }
    }
    SecureScreen()
    BackHandler {
        backClickHandler()
    }
    RevealSeedPhraseContent(
        modifier = modifier,
        mnemonicWords = state.mnemonicWordsChunked,
        passphrase = state.passphrase,
        wordsPerLine = state.seedPhraseWordsPerLine,
        onBackClick = backClickHandler,
    )
    val dialogState = state.showConfirmSeedPhraseDialogState
    if (dialogState is RevealSeedPhraseViewModel.ConfirmSeedPhraseDialogState.Shown) {
        BasicPromptAlertDialog(
            finish = { confirmed ->
                if (confirmed) {
                    onConfirmSeedPhraseClick(dialogState.factorSourceId, dialogState.mnemonicSize)
                } else {
                    onBackClick()
                }
                viewModel.dismissConfirmSeedPhraseDialog()
            },
            titleText = stringResource(id = R.string.revealSeedPhrase_warningDialog_title),
            messageText = stringResource(id = R.string.revealSeedPhrase_warningDialog_subtitle),
            confirmText = stringResource(id = R.string.revealSeedPhrase_warningDialog_confirmButton)
        )
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                RevealSeedPhraseViewModel.Effect.Close -> onBackClick()
            }
        }
    }
}

@Composable
private fun RevealSeedPhraseContent(
    modifier: Modifier = Modifier,
    mnemonicWords: PersistentList<PersistentList<String>>,
    passphrase: String,
    wordsPerLine: Int,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.displayMnemonics_cautionAlert_revealButtonLabel),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
        ) {
            item {
                WarningText(
                    modifier = Modifier.fillMaxWidth(),
                    text = AnnotatedString(stringResource(R.string.revealSeedPhrase_warning)),
                    contentColor = RadixTheme.colors.orange1
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            itemsIndexed(mnemonicWords) { outerIndex, wordsChunk ->
                SeedPhraseRow(
                    modifier = Modifier.fillMaxWidth(),
                    wordsChunk = wordsChunk,
                    wordsPerLine = wordsPerLine,
                    outerIndex = outerIndex
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            if (passphrase.isNotEmpty()) {
                item {
                    SeedPhraseSingleWord(
                        label = stringResource(id = R.string.revealSeedPhrase_passphrase),
                        word = passphrase
                    )
                }
            }

            item {
                CopySeedPhraseButton(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    mnemonicWords = mnemonicWords
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RevealSeedPhrasePreview() {
    RadixWalletPreviewTheme {
        RevealSeedPhraseContent(
            onBackClick = {},
            mnemonicWords = persistentListOf(
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo")
            ),
            passphrase = "test phrase",
            wordsPerLine = 3
        )
    }
}
