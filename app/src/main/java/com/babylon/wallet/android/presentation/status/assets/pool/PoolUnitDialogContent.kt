package com.babylon.wallet.android.presentation.status.assets.pool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.status.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.PoolResourcesValues
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import kotlinx.collections.immutable.toImmutableMap
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun PoolUnitDialogContent(
    modifier: Modifier = Modifier,
    args: AssetDialogArgs.Fungible,
    poolUnit: PoolUnit?,
    price: AssetPrice.PoolUnitPrice?
) {
    val resourceAddress = args.resourceAddress
    val amount = args.fungibleAmountOf(resourceAddress) ?: poolUnit?.stake?.ownedAmount
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingSemiLarge
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (poolUnit != null) {
            Thumbnail.PoolUnit(
                modifier = Modifier.size(104.dp),
                poolUnit = poolUnit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .radixPlaceholder(
                        visible = true,
                        shape = CircleShape
                    )
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        TokenBalance(
            modifier = Modifier
                .fillMaxWidth(fraction = if (poolUnit?.stake == null) 0.5f else 1f)
                .radixPlaceholder(visible = poolUnit?.stake == null),
            amount = amount,
            symbol = poolUnit?.resource?.symbol.orEmpty()
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            text = stringResource(id = R.string.account_poolUnits_details_currentRedeemableValue),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        if (poolUnit != null) {
            val resourcesWithAmount = remember(poolUnit, args) {
                poolUnit.pool?.resources?.associateWith {
                    args.fungibleAmountOf(it.resourceAddress) ?: poolUnit.resourceRedemptionValue(it)
                }.orEmpty().toImmutableMap()
            }
            PoolResourcesValues(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                resources = resourcesWithAmount,
                isCompact = false,
                fiatPrice = price
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                    .fillMaxWidth()
                    .assetOutlineBorder()
                    .padding(
                        vertical = RadixTheme.dimensions.paddingDefault,
                        horizontal = RadixTheme.dimensions.paddingSmall
                    ),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .radixPlaceholder(visible = true)
                )
                HorizontalDivider(color = RadixTheme.colors.gray4)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .radixPlaceholder(visible = true)
                )
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        if (!poolUnit?.stake?.description.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = poolUnit?.stake?.description.orEmpty(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = RadixTheme.colors.gray4
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        AddressRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            address = resourceAddress
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        AssetMetadataRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            key = stringResource(id = R.string.assetDetails_currentSupply)
        ) {
            Text(
                modifier = Modifier
                    .padding(start = RadixTheme.dimensions.paddingDefault)
                    .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                    .radixPlaceholder(visible = poolUnit?.stake?.currentSupply == null),
                text = when {
                    poolUnit?.stake?.currentSupply != null -> when (poolUnit.stake.currentSupply) {
                        BigDecimal.ZERO -> stringResource(id = R.string.assetDetails_supplyUnkown)
                        else -> poolUnit.stake.currentSupply.displayableQuantity()
                    }

                    else -> ""
                },
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End
            )
        }

        BehavioursSection(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            behaviours = poolUnit?.resource?.behaviours
        )

        TagsSection(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            tags = poolUnit?.resource?.tags,
        )
    }
}
