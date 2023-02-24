package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions

@Composable
fun PersonaPropertyRow(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Text(
            text = value,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
