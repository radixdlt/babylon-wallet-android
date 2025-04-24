package com.babylon.wallet.android.presentation.addfactorsource.device.confirmseedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.MnemonicTextFieldColors
import com.babylon.wallet.android.designsystem.composable.MnemonicWordTextField
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.iconRes
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConfirmDeviceSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfirmDeviceSeedPhraseViewModel,
    onDismiss: () -> Unit,
    onConfirmed: (FactorSourceKind, MnemonicWithPassphrase) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfirmDeviceSeedPhraseContent(
        modifier = modifier,
        state = state,
        onConfirmClick = viewModel::onConfirmClick,
        onDebugFillWordsClick = viewModel::onDebugFillWordsClick,
        onWordChanged = viewModel::onWordChanged,
        onDismiss = onDismiss
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ConfirmDeviceSeedPhraseViewModel.Event.Confirmed -> onConfirmed(state.factorSourceKind, event.mnemonicWithPassphrase)
            }
        }
    }
}

@Composable
private fun ConfirmDeviceSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: ConfirmDeviceSeedPhraseViewModel.State,
    onWordChanged: (Int, String) -> Unit,
    onConfirmClick: () -> Unit,
    onDebugFillWordsClick: () -> Unit,
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
                enabled = state.isConfirmButtonEnabled,
                additionalBottomContent = if (BuildConfig.DEBUG_MODE) {
                    {
                        RadixTextButton(
                            text = "(DEBUG) Fill",
                            onClick = onDebugFillWordsClick
                        )
                    }
                } else {
                    null
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = state.factorSourceKind.iconRes()),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                text = stringResource(id = R.string.newBiometricFactor_confirmSeedPhrase_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            Text(
                text = stringResource(id = R.string.newBiometricFactor_confirmSeedPhrase_subtitle, state.words.size),
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
                    error = if (word.state == SeedPhraseWord.State.Invalid) {
                        stringResource(
                            id = R.string.newBiometricFactor_confirmSeedPhrase_incorrectWord
                        )
                    } else {
                        null
                    },
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
            onDebugFillWordsClick = {},
            onWordChanged = { _, _ -> },
            onDismiss = {}
        )
    }
}
