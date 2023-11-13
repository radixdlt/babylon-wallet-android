package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.MnemonicWordTextField
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.MockUiProvider
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SeedPhraseInputForm(
    modifier: Modifier = Modifier,
    seedPhraseWords: ImmutableList<SeedPhraseWord>,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    bip39Passphrase: String,
    onFocusedWordIndexChanged: (Int) -> Unit,
    showAdvancedMode: Boolean = true,
    highlightFields: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
    ) {
        var advancedMode by remember {
            mutableStateOf(false)
        }
        seedPhraseWords.chunked(3).forEach { wordsChunk ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                wordsChunk.forEach { word ->
                    SeedPhraseWordInput(
                        onWordChanged = onWordChanged,
                        word = word,
                        focusManager = focusManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onFocusChanged = {
                            if (it.hasFocus) {
                                onFocusedWordIndexChanged(word.index)
                            }
                        },
                        enabled = word.inputDisabled.not(),
                        highlightField = highlightFields
                    )
                }
            }
        }
        AnimatedVisibility(visible = advancedMode) {
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingMedium),
                onValueChanged = onPassphraseChanged,
                value = bip39Passphrase,
                leftLabel = LabelType.Default(stringResource(id = R.string.importMnemonic_passphrase)),
                optionalHint = stringResource(id = R.string.importMnemonic_passphraseHint),

            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        if (showAdvancedMode) {
            RadixTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    id = if (advancedMode) {
                        R.string.importMnemonic_regularModeButton
                    } else {
                        R.string.importMnemonic_advancedModeButton
                    }
                ),
                onClick = {
                    advancedMode = !advancedMode
                }
            )
        }
    }
}

@Composable
fun SeedPhraseSuggestions(
    wordAutocompleteCandidates: ImmutableList<String>,
    onCandidateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        items(wordAutocompleteCandidates) { wordCandidate ->
            RadixPrimaryButton(
                text = wordCandidate,
                onClick = {
                    onCandidateClick(wordCandidate)
                },
            )
        }
    }
}

@Composable
private fun SeedPhraseWordInput(
    onWordChanged: (Int, String) -> Unit,
    word: SeedPhraseWord,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
    onFocusChanged: ((FocusState) -> Unit)?,
    enabled: Boolean,
    highlightField: Boolean
) {
    MnemonicWordTextField(
        modifier = modifier,
        onValueChanged = {
            onWordChanged(word.index, it)
        },
        value = word.value,
        label = stringResource(id = R.string.importMnemonic_wordHeading, word.index + 1),
        trailingIcon = when (word.state) {
            SeedPhraseWord.State.Valid,
            SeedPhraseWord.State.ValidDisabled -> {
                {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(
                            id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            SeedPhraseWord.State.HasValue, SeedPhraseWord.State.Invalid -> {
                {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .throttleClickable {
                                onWordChanged(word.index, "")
                            },
                        painter = painterResource(
                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_close
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            SeedPhraseWord.State.Empty -> {
                null
            }
        },
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        keyboardOptions = KeyboardOptions(
            imeAction = when {
                word.lastWord -> {
                    ImeAction.Done
                }
                word.inputDisabled -> {
                    ImeAction.None
                }
                else -> {
                    ImeAction.Next
                }
            }
        ),
        error = if (word.state == SeedPhraseWord.State.Invalid) stringResource(id = R.string.common_invalid) else null,
        onFocusChanged = onFocusChanged,
        errorFixedSize = true,
        singleLine = true,
        enabled = enabled,
        highlightField = highlightField
    )
}

@Preview(showBackground = true)
@Composable
fun InputSeedPhrasePagePreview() {
    RadixWalletTheme {
        SeedPhraseInputForm(
            seedPhraseWords = MockUiProvider.seedPhraseWords,
            onWordChanged = { _, _ -> },
            onPassphraseChanged = { },
            bip39Passphrase = "",
            onFocusedWordIndexChanged = { },
            modifier = Modifier
        )
    }
}
