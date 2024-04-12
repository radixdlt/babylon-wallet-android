package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import rdx.works.core.domain.DApp

@Composable
fun InvolvedComponentDetails(modifier: Modifier = Modifier, dApp: DApp?, text: String, iconSize: Dp = 24.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.DApp(
            modifier = Modifier.size(iconSize),
            dapp = dApp
        )
        Text(
            text = text,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
    }
}
