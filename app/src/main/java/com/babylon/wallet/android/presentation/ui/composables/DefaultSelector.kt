package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T : SelectorItem<*>> DefaultSelector(
    modifier: Modifier,
    items: ImmutableList<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    var isMenuExpanded by remember {
        mutableStateOf(false)
    }
    Box {
        Row(
            modifier = modifier
                .throttleClickable {
                    isMenuExpanded = true
                }
                .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                .border(1.dp, RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = selectedItem.label,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }

        DropdownMenu(
            modifier = Modifier.background(RadixTheme.colors.gray5),
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = item.label,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.defaultText
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onItemSelected(item)
                    },
                    contentPadding = PaddingValues(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                )
            }
        }
    }
}

data class SelectorItem<T>(
    val item: T,
    val label: String
)
