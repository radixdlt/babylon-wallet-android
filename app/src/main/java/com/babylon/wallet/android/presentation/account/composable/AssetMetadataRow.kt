package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import java.util.Locale

@Composable
fun AssetMetadataRow(modifier: Modifier, key: String, value: String) {
    Row(
        modifier,
        horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            text = key.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End
        )
    }
}
