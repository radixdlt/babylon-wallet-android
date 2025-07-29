package com.babylon.wallet.android.presentation.addfactorsource.device.seedphrase

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.MnemonicTextFieldColors
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
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
import com.radixdlt.sargon.Bip39WordCount
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
        onNumberOfWordsChanged = viewModel::onNumberOfWordsChanged,
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
    onNumberOfWordsChanged: (Bip39WordCount) -> Unit,
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
        containerColor = RadixTheme.colors.background,
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
                text = when (state.context) {
                    AddFactorSourceInput.Context.New -> stringResource(id = R.string.newBiometricFactor_seedPhrase_title)
                    is AddFactorSourceInput.Context.Recovery -> stringResource(id = R.string.enterSeedPhrase_bip39Instruction)
                },
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                text = when (state.context) {
                    AddFactorSourceInput.Context.New -> stringResource(id = R.string.newBiometricFactor_seedPhrase_subtitle)
                    is AddFactorSourceInput.Context.Recovery -> "Enter your BIP39 seed phrase. Make sure it's backed up securely and accessible only to you." // TODO localise
                },
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            if (state.isOlympiaRecovery) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                val tabs = remember {
                    Bip39WordCount.entries.sortedByDescending { it.value }
                }
                var tabIndex by remember { mutableStateOf(0) }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = RadixTheme.dimensions.paddingSmall
                        ),
                    text = stringResource(id = R.string.importMnemonic_numberOfWordsPicker),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                TabRow(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .background(RadixTheme.colors.backgroundTertiary, RadixTheme.shapes.roundedRectSmall),
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[tabIndex])
                                .fillMaxHeight()
                                .zIndex(-1f)
                                .padding(2.dp)
                                .background(
                                    color = RadixTheme.colors.selectedSegmentedControl,
                                    shape = RadixTheme.shapes.roundedRectSmall
                                )
                        )
                    }
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tabs.indexOf(tab) == tabIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        Tab(
                            modifier = Modifier.wrapContentWidth(),
                            selected = isSelected,
                            onClick = {
                                tabIndex = tabs.indexOf(tab)
                                onNumberOfWordsChanged(tab)
                            },
                            interactionSource = interactionSource,
                            selectedContentColor = RadixTheme.colors.text,
                            unselectedContentColor = RadixTheme.colors.textSecondary
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                                text = tab.value.toString(),
                                style = RadixTheme.typography.body1HighImportance,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))

            SeedPhraseInputView(
                modifier = Modifier.fillMaxWidth(),
                seedPhraseWords = state.seedPhraseState.seedPhraseWords,
                onWordChanged = onWordChanged,
                onFocusedWordIndexChanged = { focusedWordIndex = it },
                initiallyFocusedIndex = focusedWordIndex,
                textFieldColors = MnemonicTextFieldColors.default()
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
        ErrorAlertDialog(
            cancel = onDismissMessage,
            errorMessage = error,
            cancelMessage = stringResource(id = R.string.common_close)
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
            onNumberOfWordsChanged = {},
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
                context = AddFactorSourceInput.Context.New,
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
                context = AddFactorSourceInput.Context.Recovery(
                    isOlympia = true
                ),
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
                context = AddFactorSourceInput.Context.New,
                errorMessage = UiMessage.ErrorMessage(RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse)
            )
        )
}
