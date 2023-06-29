package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun InfoLink(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.blue1,
    textStyle: TextStyle = RadixTheme.typography.body1StandaloneLink,
    iconRes: Int = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = iconRes
            ),
            contentDescription = null,
            tint = contentColor
        )
        Text(
            text = text,
            style = textStyle,
            color = contentColor
        )
    }
}
