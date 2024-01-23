package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.metadata.name
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.InvolvedComponentDetails
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun PoolsContent(
    modifier: Modifier = Modifier,
    text: String,
    pools: ImmutableList<Pool>,
    onDAppClick: (DApp) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .dashedCircleBorder(RadixTheme.colors.gray3),
                painter = painterResource(id = DSR.ic_pools_contribution),
                contentDescription = null,
                colorFilter = ColorFilter.tint(RadixTheme.colors.gray2),
                contentScale = ContentScale.Inside
            )
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Text(
                    modifier = Modifier.weight(1f, false),
                    maxLines = 2,
                    text = text,
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2,
                    overflow = TextOverflow.Ellipsis,
                )
                val iconRes = if (expanded) {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
                } else {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    tint = RadixTheme.colors.gray2,
                    contentDescription = "arrow"
                )
            }
            if (expanded) {
                StrokeLine(modifier = Modifier.padding(end = RadixTheme.dimensions.paddingLarge), height = 60.dp)
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    val (associatedDApps, unknownPoolsCount) = remember(pools) {
        pools.mapNotNull { it.associatedDApp } to pools.count { it.associatedDApp == null }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .fillMaxWidth()
        ) {
            associatedDApps.forEach { dApp ->
                InvolvedComponentDetails(
                    iconSize = 44.dp,
                    dApp = dApp,
                    text = dApp.name.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .clickable { onDAppClick(dApp) }
                        .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingDefault)
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
            if (unknownPoolsCount > 0) {
                InvolvedComponentDetails(
                    iconSize = 44.dp,
                    dApp = null,
                    text = stringResource(id = R.string.transactionReview_unknownPools, unknownPoolsCount),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingDefault)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PoolsContentPreview() {
    RadixWalletTheme {
        Column {
            PoolsContent(
                text = "Contributing to pools".uppercase(),
                pools = persistentListOf(),
                onDAppClick = {}
            )
        }
    }
}
