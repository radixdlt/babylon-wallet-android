package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SeedPhraseRow(
    wordsChunk: ImmutableList<String>,
    outerIndex: Int,
    wordsPerLine: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        wordsChunk.forEachIndexed { index, word ->
            SeedPhraseSingleWord(
                modifier = Modifier.weight(1f),
                label = stringResource(
                    id = R.string.revealSeedPhrase_wordLabel,
                    outerIndex * wordsPerLine + index + 1
                ),
                word = word
            )
        }
    }
}

@Composable
fun CopySeedPhraseButton(
    modifier: Modifier = Modifier,
    mnemonicWords: ImmutableList<ImmutableList<String>>
) {
    if (BuildConfig.DEBUG_MODE) {
        val clipboardManager = LocalClipboardManager.current

        RadixPrimaryButton(
            modifier = modifier,
            text = "(DEBUG) Copy seed phrase",
            onClick = {
                val phrase = mnemonicWords.joinToString(" ") { it.joinToString(" ") }
                clipboardManager.setText(buildAnnotatedString { append(phrase) })
            }
        )
    }
}

@Composable
fun SeedPhraseSingleWord(
    modifier: Modifier = Modifier,
    label: String,
    word: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        Text(
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.backgroundTertiary, shape = RadixTheme.shapes.roundedRectSmall)
                .border(1.dp, RadixTheme.colors.backgroundTertiary, shape = RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingMedium),
            text = word,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
    }
}
