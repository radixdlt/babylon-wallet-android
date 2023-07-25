package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RedWarningText(
    modifier: Modifier = Modifier,
    text: AnnotatedString
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            ),
            contentDescription = null,
            tint = RadixTheme.colors.red1
        )
        androidx.compose.material3.Text(
            text = text,
            style = RadixTheme.typography.body1StandaloneLink,
            color = RadixTheme.colors.red1
        )
    }
}
