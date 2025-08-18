package com.babylon.wallet.android.presentation.common.seedphrase

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.radixdlt.sargon.Bip39WordCount
import kotlinx.collections.immutable.ImmutableList

@Composable
fun NumberOfWordsTabView(
    options: ImmutableList<Bip39WordCount>,
    currentNumberOfWords: Int,
    onNumberOfWordsChanged: (Bip39WordCount) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember(options) {
        options.sortedByDescending { it.value }
    }
    var tabIndex by remember(currentNumberOfWords) {
        mutableIntStateOf(
            options.indexOfFirst {
                it.value == currentNumberOfWords.toUByte()
            }.takeIf { it != -1 } ?: 0
        )
    }

    Column(
        modifier = modifier
    ) {
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
}
