package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RevealSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: RevealSeedPhraseViewModel,
    onBackClick: () -> Unit
) {
    var showWarningDialog by remember {
        mutableStateOf(false)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backClickHandler = {
        if (state.backedUp) {
            onBackClick()
        } else {
            showWarningDialog = true
        }
    }
    SecureScreen()
    BackHandler {
        backClickHandler()
    }
    RevealSeedPhraseContent(
        modifier = modifier,
        mnemonicWords = state.mnemonicWords,
        passphrase = state.passphrase,
        seedPhraseWordsPerLine = state.seedPhraseWordsPerLine,
        onBackClick = backClickHandler,
    )
    if (showWarningDialog) {
        BasicPromptAlertDialog(
            finish = { confirmed ->
                showWarningDialog = false
                if (confirmed) {
                    viewModel.markFactorSourceBackedUp()
                } else {
                    onBackClick()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.revealSeedPhrase_warningDialog_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.revealSeedPhrase_warningDialog_subtitle),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
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
    seedPhraseWordsPerLine: Int,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.displayMnemonics_cautionAlert_revealButtonLabel),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    InfoLink(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.revealSeedPhrase_warning),
                        contentColor = RadixTheme.colors.orange1,
                        iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
                itemsIndexed(mnemonicWords) { outerIndex, wordsChunk ->
                    SeedPhraseRow(
                        modifier = Modifier.fillMaxWidth(),
                        wordsChunk = wordsChunk,
                        seedPhraseWordsPerLine = seedPhraseWordsPerLine,
                        outerIndex = outerIndex
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
                if (passphrase.isNotEmpty()) {
                    item {
                        SingleWord(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.revealSeedPhrase_passphrase),
                            word = passphrase
                        )
                    }
                }
                if (BuildConfig.DEBUG_MODE) {
                    item {
                        val clipboardManager = LocalClipboardManager.current
                        RadixPrimaryButton(text = "(DEBUG) Copy seed phrase", onClick = {
                            val phrase = mnemonicWords.joinToString(" ") { it.joinToString(" ") }
                            clipboardManager.setText(buildAnnotatedString { append(phrase) })
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedPhraseRow(
    wordsChunk: ImmutableList<String>,
    outerIndex: Int,
    seedPhraseWordsPerLine: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        wordsChunk.forEachIndexed { index, word ->
            SingleWord(
                modifier = Modifier.weight(1f),
                label = stringResource(
                    id = R.string.revealSeedPhrase_wordLabel,
                    outerIndex * seedPhraseWordsPerLine + index + 1
                ),
                word = word
            )
        }
    }
}

@Composable
private fun SingleWord(label: String, word: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)) {
        Text(
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingMedium),
            text = word,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RevealSeedPhrasePreview() {
    RadixWalletTheme {
        RevealSeedPhraseContent(
            onBackClick = {},
            mnemonicWords = persistentListOf(
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo"),
                persistentListOf("zoo", "zoo", "zoo")
            ),
            passphrase = "test phrase",
            seedPhraseWordsPerLine = 3
        )
    }
}
