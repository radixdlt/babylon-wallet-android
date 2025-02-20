package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.MnemonicTextFieldColors
import com.babylon.wallet.android.designsystem.composable.MnemonicWordTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConfirmDeviceSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfirmDeviceSeedPhraseViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfirmDeviceSeedPhraseContent(
        modifier = modifier,
        state = state,
        onConfirmClick = viewModel::onConfirmClick,
        onWordChanged = viewModel::onWordChanged,
        onDismiss = onDismiss
    )
}

@Composable
private fun ConfirmDeviceSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: ConfirmDeviceSeedPhraseViewModel.State,
    onWordChanged: (Int, String) -> Unit,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = { onDismiss() },
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
        bottomBar = {
            RadixBottomBar(
                onClick = onConfirmClick,
                text = stringResource(id = R.string.common_confirm),
                enabled = state.isConfirmButtonEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Confirm Seed Phrase", //TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            Text(
                text = "Enter ${state.words.size} words from your seed phrase to confirm you’ve recorded it correctly.", //TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            state.words.forEachIndexed { index, word ->
                MnemonicWordTextField(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingMedium),
                    onValueChanged = { onWordChanged(word.index, it) },
                    value = word.value,
                    label = stringResource(id = R.string.importMnemonic_wordHeading, word.index + 1),
                    keyboardOptions = KeyboardOptions(
                        imeAction = when {
                            word.lastWord -> ImeAction.Done
                            else -> ImeAction.Next
                        }
                    ),
                    error = if (word.state == SeedPhraseWord.State.Invalid) "Incorrect. Try again" else null, //TODO crowdin
                    errorFixedSize = true,
                    singleLine = true,
                    hasInitialFocus = index == 0,
                    colors = MnemonicTextFieldColors.default().copy(
                        errorTextColor = RadixTheme.colors.gray1,
                        errorHintColor = RadixTheme.colors.red1
                    )
                )
            }
        }
    }
}

@UsesSampleValues
@Composable
@Preview
private fun ConfirmDeviceSeedPhrasePreview() {
    RadixWalletPreviewTheme {
        ConfirmDeviceSeedPhraseContent(
            state = ConfirmDeviceSeedPhraseViewModel.State(
                words = persistentListOf(
                    SeedPhraseWord(
                        index = 2,
                        value = "ship",
                        state = SeedPhraseWord.State.Valid,
                        lastWord = false
                    ),
                    SeedPhraseWord(
                        index = 4,
                        value = "shipping",
                        state = SeedPhraseWord.State.Invalid,
                        lastWord = false
                    ),
                    SeedPhraseWord(
                        index = 9,
                        value = "value2",
                        state = SeedPhraseWord.State.Invalid,
                        lastWord = false
                    ),
                    SeedPhraseWord(
                        index = 23,
                        value = "",
                        state = SeedPhraseWord.State.Empty,
                        lastWord = true
                    )
                )
            ),
            onConfirmClick = {},
            onWordChanged = { _, _ -> },
            onDismiss = {}
        )
    }
}