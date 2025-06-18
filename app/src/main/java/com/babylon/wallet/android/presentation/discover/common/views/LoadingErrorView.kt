package com.babylon.wallet.android.presentation.discover.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR

@Composable
fun LoadingErrorView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    LoadingErrorView(
        modifier = modifier,
        title = title,
        subtitle = {
            Text(
                text = subtitle,
                color = RadixTheme.colors.textSecondary,
                style = RadixTheme.typography.body2Regular
            )
        }
    )
}

@Composable
fun LoadingErrorView(
    title: String,
    subtitle: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            modifier = Modifier.size(44.dp),
            painter = painterResource(DSR.ic_factory_reset),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )

        Text(
            text = title,
            color = RadixTheme.colors.text,
            style = RadixTheme.typography.body1HighImportance
        )

        subtitle()
    }
}