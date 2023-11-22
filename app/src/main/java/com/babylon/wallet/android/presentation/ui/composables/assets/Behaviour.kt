package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun Behaviour(
    modifier: Modifier = Modifier,
    icon: Painter,
    name: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = "behaviour image"
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = name,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1,
        )

//        Icon(
//            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
//            contentDescription = null,
//            tint = RadixTheme.colors.gray3
//        )
    }
}
