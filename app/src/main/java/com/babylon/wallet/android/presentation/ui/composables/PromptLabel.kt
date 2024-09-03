package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun PromptLabel(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = RadixTheme.colors.orange3,
    @DrawableRes iconRes: Int = R.drawable.ic_warning_error,
    iconTint: Color = RadixTheme.colors.orange3,
    iconSize: Dp = 24.dp,
    endContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint
        )

        Text(
            text = text,
            style = RadixTheme.typography.body2HighImportance,
            color = textColor
        )

        endContent?.let {
            Spacer(modifier = Modifier.weight(1f))

            it()
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun AccountPromptLabelPreview() {
    RadixWalletPreviewTheme {
        PromptLabel(
            text = "Problem with Configuration Backup"
        )
    }
}
