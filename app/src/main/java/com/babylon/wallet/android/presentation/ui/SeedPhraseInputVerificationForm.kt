package com.babylon.wallet.android.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.MnemonicWordTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SeedPhraseInputVerificationForm(
    modifier: Modifier = Modifier,
    seedPhraseWords: ImmutableList<SeedPhraseWord>,
    onWordChanged: (Int, String) -> Unit
) {
    val firstFocusedIndex by remember(seedPhraseWords) {
        mutableIntStateOf(seedPhraseWords.indexOfFirst { it.state == SeedPhraseWord.State.Empty })
    }
    Column(
        modifier = modifier
    ) {
        seedPhraseWords.chunked(3).forEachIndexed { index, wordsChunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                wordsChunk.forEachIndexed { innerIndex, word ->
                    val wordIndex = index * 3 + innerIndex
                    SeedPhraseWordInput(
                        onWordChanged = onWordChanged,
                        word = word,
                        modifier = Modifier.weight(1f),
                        enabled = word.inputDisabled.not(),
                        initiallyFocused = firstFocusedIndex == wordIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun SeedPhraseWordInput(
    onWordChanged: (Int, String) -> Unit,
    word: SeedPhraseWord,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    initiallyFocused: Boolean
) {
    MnemonicWordTextField(
        modifier = modifier,
        onValueChanged = {
            onWordChanged(word.index, it)
        },
        value = word.value,
        label = stringResource(id = R.string.importMnemonic_wordHeading, word.index + 1),
        trailingIcon = when (word.state) {
            SeedPhraseWord.State.NotEmpty -> {
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
            else -> {
                null
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = when {
                word.lastWord -> {
                    ImeAction.Done
                }
                else -> {
                    ImeAction.Next
                }
            }
        ),
        errorFixedSize = true,
        singleLine = true,
        enabled = enabled,
        masked = !enabled,
        highlightField = true,
        hasInitialFocus = initiallyFocused,
        hintColor = if (enabled.not()) RadixTheme.colors.gray4 else null
    )
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SeedPhraseInputVerificationFormPreview() {
    RadixWalletTheme {
        SeedPhraseInputVerificationForm(
            seedPhraseWords = MockUiProvider.seedPhraseWords,
            onWordChanged = { _, _ -> },
            modifier = Modifier
        )
    }
}
