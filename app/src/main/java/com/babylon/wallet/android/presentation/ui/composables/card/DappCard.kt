package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import rdx.works.core.domain.DApp

@Composable
fun DappCard(
    modifier: Modifier = Modifier,
    dApp: DApp,
    showChevron: Boolean = true,
    bottomContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .defaultCardShadow()
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(RadixTheme.colors.card, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.DApp(
                modifier = Modifier.size(44.dp),
                dapp = dApp,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            Text(
                modifier = Modifier.weight(1f),
                text = dApp.displayName(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                    ),
                    contentDescription = null,
                    tint = RadixTheme.colors.icon
                )
            }
        }

        bottomContent?.let {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            it()
        }
    }
}
