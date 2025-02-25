package com.babylon.wallet.android.presentation.addfactorsource.device.seedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.MnemonicTextFieldColors
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputView
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.rememberSuggestionsVisibilityState
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.HideKeyboardOnFullScroll
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.toPersistentList

@Composable
fun DeviceSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceSeedPhraseViewModel,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DeviceSeedPhraseContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onDismissMessage = viewModel::onDismissMessage,
        onWordChanged = viewModel::onWordChanged,
        onWordSelected = viewModel::onWordSelected,
        onEnterCustomSeedPhraseClick = viewModel::onEnterCustomSeedPhraseClick,
        onConfirmClick = viewModel::onConfirmClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DeviceSeedPhraseViewModel.Event.Confirmed -> onConfirmed()
                DeviceSeedPhraseViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }
}

@Composable
private fun DeviceSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: DeviceSeedPhraseViewModel.State,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
    onWordChanged: (Int, String) -> Unit,
    onWordSelected: (Int, String) -> Unit,
    onEnterCustomSeedPhraseClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    SecureScreen()

    val scrollState = rememberScrollState()
    var focusedWordIndex by remember { mutableStateOf<Int?>(null) }
    val isSuggestionsVisible = state.seedPhraseState.rememberSuggestionsVisibilityState()

    HideKeyboardOnFullScroll(scrollState)

    LaunchedEffect(state.isEditingEnabled) {
        if (state.isEditingEnabled) {
            scrollState.animateScrollTo(0)
            focusedWordIndex = 0
        }
    }

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
            if (isSuggestionsVisible) {
                SeedPhraseSuggestions(
                    modifier = Modifier
                        .imePadding()
                        .fillMaxWidth()
                        .height(RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight)
                        .padding(RadixTheme.dimensions.paddingSmall),
                    wordAutocompleteCandidates = state.seedPhraseState.wordAutocompleteCandidates,
                    onCandidateClick = { candidate ->
                        focusedWordIndex?.let {
                            onWordSelected(it, candidate)
                        }
                    }
                )
            } else {
                RadixBottomBar(
                    onClick = onConfirmClick,
                    text = stringResource(id = R.string.common_confirm),
                    enabled = state.isConfirmButtonEnabled
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = if (isSuggestionsVisible) {
                        RadixTheme.dimensions.seedPhraseWordsSuggestionsHeight
                    } else {
                        RadixTheme.dimensions.paddingDefault
                    }
                )
                .verticalScroll(scrollState)
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.newBiometricFactor_seedPhrase_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.newBiometricFactor_seedPhrase_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))

            SeedPhraseInputView(
                modifier = Modifier.fillMaxWidth(),
                seedPhraseWords = state.seedPhraseState.seedPhraseWords,
                onWordChanged = onWordChanged,
                onFocusedWordIndexChanged = { focusedWordIndex = it },
                initiallyFocusedIndex = focusedWordIndex,
                textFieldColors = MnemonicTextFieldColors.default().copy(
                    disabledTextColor = RadixTheme.colors.gray1,
                    disabledBorderColor = Color.Transparent,
                    highlightedBorderColor = Color.Transparent
                )
            )

            val shouldDisplaySeedPhraseWarning = remember(state.seedPhraseState) {
                state.seedPhraseState.shouldDisplayInvalidSeedPhraseWarning()
            }

            if (shouldDisplaySeedPhraseWarning) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                WarningText(
                    modifier = Modifier.fillMaxWidth(),
                    text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure))
                )
            }

            if (!state.isEditingEnabled) {
                RadixTextButton(
                    text = stringResource(id = R.string.newBiometricFactor_seedPhrase_enterCustomButton),
                    onClick = onEnterCustomSeedPhraseClick
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
    }

    state.errorMessage?.let { error ->
        BasicPromptAlertDialog(
            finish = { onDismissMessage() },
            messageText = error.getMessage(),
            confirmText = stringResource(id = R.string.common_close),
            dismissText = null
        )
    }
}

@UsesSampleValues
@Composable
@Preview
private fun DeviceSeedPhrasePreview(
    @PreviewParameter(DeviceSeedPhrasePreviewProvider::class) state: DeviceSeedPhraseViewModel.State
) {
    RadixWalletPreviewTheme {
        DeviceSeedPhraseContent(
            state = state,
            onDismiss = {},
            onDismissMessage = {},
            onWordChanged = { _, _ -> },
            onWordSelected = { _, _ -> },
            onConfirmClick = {},
            onEnterCustomSeedPhraseClick = {},
        )
    }
}

@UsesSampleValues
class DeviceSeedPhrasePreviewProvider : PreviewParameterProvider<DeviceSeedPhraseViewModel.State> {

    override val values: Sequence<DeviceSeedPhraseViewModel.State>
        get() = sequenceOf(
            DeviceSeedPhraseViewModel.State(
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
            DeviceSeedPhraseViewModel.State(
                seedPhraseState = SeedPhraseInputDelegate.State(
                    seedPhraseWords = Mnemonic.sample().words.mapIndexed { index, bip39Word ->
                        SeedPhraseWord(
                            index = index,
                            value = bip39Word.word,
                            state = SeedPhraseWord.State.Valid
                        )
                    }.toPersistentList()
                ),
                isEditingEnabled = true
            ),
            DeviceSeedPhraseViewModel.State(
                errorMessage = UiMessage.ErrorMessage(RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse)
            )
        )
}
