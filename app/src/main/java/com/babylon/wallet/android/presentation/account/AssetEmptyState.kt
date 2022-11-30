package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun AssetEmptyState(title: String, subtitle: String, modifier: Modifier, onInfoClick: () -> Unit) {
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            Row(
                modifier = Modifier.clickable { onInfoClick() },
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_outline),
                    contentDescription = null,
                    tint = RadixTheme.colors.blue2
                )
                Text(
                    text = subtitle,
                    style = RadixTheme.typography.body1StandaloneLink,
                    color = RadixTheme.colors.blue2
                )
            }
        }
    }
}
