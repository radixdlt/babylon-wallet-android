package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.Red1
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun ApplySecuritySettingsLabel(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    labelColor: Color = RadixTheme.colors.white.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier
            .background(labelColor, RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall)
            .throttleClickable {
                onClick()
            }
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = R.drawable.ic_security),
            contentDescription = null,
            tint = RadixTheme.colors.white
        )
        Text(
            text = text,
            style = RadixTheme.typography.body2HighImportance,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            color = RadixTheme.colors.white
        )
        Badge(backgroundColor = Red1)
    }
}
