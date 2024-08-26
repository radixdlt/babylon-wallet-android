package com.babylon.wallet.android.presentation.dialogs.assets.pool

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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.dialogs.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.dialogs.assets.BehavioursSection
import com.babylon.wallet.android.presentation.dialogs.assets.DescriptionSection
import com.babylon.wallet.android.presentation.dialogs.assets.NonStandardMetadataSection
import com.babylon.wallet.android.presentation.dialogs.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.PoolResourcesValues
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.composables.resources.TokenBalance
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.collections.immutable.toImmutableMap
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.PoolUnit

@Composable
fun PoolUnitDialogContent(
    modifier: Modifier = Modifier,
    args: AssetDialogArgs.Fungible,
    poolUnit: PoolUnit?,
    poolUnitPrice: AssetPrice.PoolUnitPrice?,
    isLoadingBalance: Boolean,
    canBeHidden: Boolean,
    onHideClick: () -> Unit
) {
    val resourceAddress = args.resourceAddress
    val amount = args.fungibleAmountOf(resourceAddress) ?: poolUnit?.stake?.ownedAmount
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = RadixTheme.dimensions.paddingSemiLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
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
                        args.fungibleAmountOf(it.address) ?: poolUnit.resourceRedemptionValue(it)
                    }.orEmpty().toImmutableMap()
                }
                PoolResourcesValues(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    resources = resourcesWithAmount,
                    isCompact = false,
                    poolUnitPrice = poolUnitPrice,
                    isLoadingBalance = isLoadingBalance
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

            DescriptionSection(
                modifier = Modifier.fillMaxWidth(),
                description = poolUnit?.stake?.description,
                infoUrl = poolUnit?.stake?.infoUrl
            )

            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                address = Address.Resource(resourceAddress)
            )
            if (!poolUnit?.resource?.name.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_name)
                ) {
                    Text(
                        text = poolUnit?.resource?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            MetadataView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                key = stringResource(id = R.string.assetDetails_currentSupply)
            ) {
                Text(
                    modifier = Modifier
                        .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                        .radixPlaceholder(visible = poolUnit?.stake?.currentSupply == null),
                    text = when (val supply = poolUnit?.stake?.currentSupply) {
                        null -> stringResource(id = R.string.empty)
                        0.toDecimal192() -> stringResource(id = R.string.assetDetails_supplyUnkown)
                        else -> supply.formatted()
                    },
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End
                )
            }

            poolUnit?.resource?.divisibility?.let {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                    key = stringResource(id = R.string.assetDetails_divisibility)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2),
                        text = it.value.toString(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }
            }

            BehavioursSection(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                behaviours = poolUnit?.resource?.behaviours
            )

            TagsSection(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
                tags = poolUnit?.resource?.tags
            )

            poolUnit?.resource?.let { resource ->
                NonStandardMetadataSection(resource = resource)
            }
        }

        if (canBeHidden) {
            Spacer(modifier = Modifier.weight(1f))

            RadixBottomBar(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingLarge),
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.assetDetails_hideAsset_button),
                        onClick = onHideClick
                    )
                }
            )
        }
    }
}
