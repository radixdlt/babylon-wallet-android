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
import androidx.compose.ui.tooling.preview.Preview
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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.composables.LargeBoundedAmountSection
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.PoolResourcesValues
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.xrd
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.toImmutableMap
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata

@Composable
fun PoolUnitDialogContent(
    modifier: Modifier = Modifier,
    args: AssetDialogArgs.Fungible,
    poolUnit: PoolUnit?,
    poolUnitPrice: AssetPrice.PoolUnitPrice?,
    isLoadingBalance: Boolean,
    canBeHidden: Boolean,
    onInfoClick: (GlossaryItem) -> Unit,
    onHideClick: () -> Unit
) {
    val resourceAddress = args.resourceAddress
    val amount = remember(args) { args.fungibleAmountOf(resourceAddress) }
        ?: remember(poolUnit) { poolUnit?.stake?.ownedAmount?.let { BoundedAmount.Exact(it) } }
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
            LargeBoundedAmountSection(
                modifier = Modifier
                    .widthIn(min = if (poolUnit == null) RadixTheme.dimensions.amountShimmeringWidth else 0.dp)
                    .radixPlaceholder(visible = poolUnit == null),
                boundedAmount = amount,
                symbol = poolUnit?.resource?.symbol
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
                    poolUnit.pool?.resources?.associateWith { resource ->
                        args.fungibleAmountOf(resource.address)
                            ?: poolUnit.poolItemRedemptionValue(resource.address)?.let { BoundedAmount.Exact(it) }
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
                behaviours = poolUnit?.resource?.behaviours,
                onInfoClick = onInfoClick
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
                        text = stringResource(id = R.string.assetDetails_hideAsset),
                        onClick = onHideClick
                    )
                }
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun PoolUnitDialogContentPreview() {
    RadixWalletPreviewTheme {
        PoolUnitDialogContent(
            args = AssetDialogArgs.Fungible(
                resourceAddress = ResourceAddress.xrd(NetworkId.MAINNET),
                isNewlyCreated = false,
                underAccountAddress = null,
                amounts = mapOf(
                    ResourceAddress.xrd(NetworkId.MAINNET).string to BoundedAmount.Exact(Decimal192.sample()),
                    ResourceAddress.sampleMainnet.other().string to BoundedAmount.Max(Decimal192.sample()),
                )
            ),
            poolUnit = PoolUnit(
                stake = Resource.FungibleResource(
                    address = ResourceAddress.xrd(NetworkId.MAINNET),
                    ownedAmount = 123.toDecimal192(),
                    currentSupply = Decimal192.sample.invoke(),
                    assetBehaviours = setOf(AssetBehaviour.SUPPLY_INCREASABLE, AssetBehaviour.SUPPLY_FLEXIBLE),
                    metadata = listOf(
                        Metadata.Collection(
                            key = ExplicitMetadataKey.TAGS.key,
                            values = listOf(),
                        )
                    )
                ),
                pool = Pool.sampleMainnet()
            ),
            poolUnitPrice = null,
            isLoadingBalance = false,
            canBeHidden = false,
            onInfoClick = {},
            onHideClick = {}
        )
    }
}
