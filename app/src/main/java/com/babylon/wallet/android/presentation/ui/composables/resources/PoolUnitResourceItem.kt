package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.PoolUnit
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun PoolUnitItem(
    resource: PoolUnit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RadixTheme.shapes.roundedRectMedium,
        colors = CardDefaults.cardColors(
            containerColor = RadixTheme.colors.defaultBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Thumbnail.PoolUnit(
                    modifier = Modifier.size(44.dp),
                    poolUnit = resource
                )
                Text(
                    poolName(resource.poolUnitResource.displayTitle),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                PoolResourcesValues(modifier = Modifier.weight(1f), resource = resource)
                trailingContent()
            }
        }
    }
}

@Composable
fun PoolResourcesValues(resource: PoolUnit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.border(1.dp, RadixTheme.colors.gray4, RadixTheme.shapes.roundedRectMedium)) {
        val itemsSize = resource.poolResources.size
        resource.poolResources.forEachIndexed { index, poolResource ->
            Row(
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingLarge
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = poolResource
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = poolResource.displayTitle,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
                Text(
                    resource.resourceRedemptionValue(poolResource.resourceAddress)?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1
                )
            }
            if (index != itemsSize - 1) {
                Divider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Composable
fun StakeClaimNftItem(
    stakeClaimNft: Resource.NonFungibleResource.Item,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )
        Text(
            text = if (stakeClaimNft.readyToClaim) {
                stringResource(id = R.string.account_poolUnits_readyToClaim)
            } else {
                stringResource(id = R.string.account_poolUnits_unstaking)
            },
            modifier = Modifier.weight(1f),
            style = RadixTheme.typography.body2HighImportance,
            color = if (stakeClaimNft.readyToClaim) RadixTheme.colors.green1 else RadixTheme.colors.gray1,
            maxLines = 1
        )
        stakeClaimNft.claimAmountXrd?.let {
            Text(
                it.displayableQuantity(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )
        }
        trailingContent()
    }
}

@Composable
fun LiquidStakeUnitItem(
    stakeValueInXRD: BigDecimal?,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle),
            tint = Color.Unspecified
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                Resource.XRD_SYMBOL,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
            Text(
                stringResource(id = R.string.account_poolUnits_staked),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2,
                maxLines = 1
            )
        }
        Text(
            stakeValueInXRD?.displayableQuantity().orEmpty(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
        trailingContent()
    }
}
