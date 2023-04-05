package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun InfoLink(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline
            ),
            contentDescription = null,
            tint = RadixTheme.colors.blue1
        )
        Text(
            text = text,
            style = RadixTheme.typography.body1StandaloneLink,
            color = RadixTheme.colors.blue1
        )
    }
}
