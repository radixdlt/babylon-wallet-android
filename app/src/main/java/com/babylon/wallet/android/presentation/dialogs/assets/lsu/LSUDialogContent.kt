package com.babylon.wallet.android.presentation.dialogs.assets.lsu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.dialogs.assets.AssetDialogArgs
import com.babylon.wallet.android.presentation.dialogs.assets.BehavioursSection
import com.babylon.wallet.android.presentation.dialogs.assets.DescriptionSection
import com.babylon.wallet.android.presentation.dialogs.assets.NonStandardMetadataSection
import com.babylon.wallet.android.presentation.dialogs.assets.TagsSection
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.transaction.composables.CountedAmountSection
import com.babylon.wallet.android.presentation.transaction.composables.LargeCountedAmountSection
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.ValidatorDetailsItem
import com.babylon.wallet.android.presentation.ui.composables.assets.FiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.assets.assetOutlineBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.xrd
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.sargon.default

@Composable
fun LSUDialogContent(
    modifier: Modifier = Modifier,
    lsu: LiquidStakeUnit?,
    price: AssetPrice.LSUPrice?,
    args: AssetDialogArgs.Fungible,
    isLoadingBalance: Boolean,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val resourceAddress = args.resourceAddress
    val amount = remember(args) { args.fungibleAmountOf(resourceAddress) }
        ?: lsu?.fungibleResource?.ownedAmount?.let { CountedAmount.Exact(it) }
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
        LSUIconSection(lsu = lsu)
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        LargeCountedAmountSection(
            modifier = Modifier
                .widthIn(min = if (lsu == null) RadixTheme.dimensions.amountShimmeringWidth else 0.dp)
                .radixPlaceholder(visible = lsu == null),
            countedAmount = amount,
            symbol = lsu?.resource?.symbol
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

        if (lsu != null) {
            ValidatorDetailsItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                validator = lsu.validator
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                    .radixPlaceholder(visible = true)
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        val xrdWorth = remember(args, lsu) {
            val xrdResourceAddress = runCatching {
                val networkId = args.resourceAddress.networkId
                XrdResource.address(networkId = networkId)
            }.getOrNull()

            xrdResourceAddress?.let { args.fungibleAmountOf(it) } ?: lsu?.stakeValueXRD()?.let { CountedAmount.Exact(it) }
        }
        LSUResourceValue(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
            amount = xrdWorth,
            price = price,
            isLoadingBalance = isLoadingBalance
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        DescriptionSection(
            modifier = Modifier.fillMaxWidth(),
            description = lsu?.fungibleResource?.description,
            infoUrl = lsu?.fungibleResource?.infoUrl
        )

        AddressRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            address = Address.Resource(resourceAddress)
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        if (lsu != null) {
            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2),
                label = stringResource(id = R.string.assetDetails_validator),
                address = Address.Validator(lsu.validator.address)
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        MetadataView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
            key = stringResource(id = R.string.assetDetails_name)
        ) {
            Text(
                modifier = Modifier
                    .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                    .radixPlaceholder(visible = lsu == null),
                text = lsu?.name().orEmpty(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End
            )
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
                    .radixPlaceholder(
                        visible = lsu?.fungibleResource?.currentSupply == null
                    ),
                text = when (val supply = lsu?.fungibleResource?.currentSupply) {
                    null -> stringResource(id = R.string.empty)
                    0.toDecimal192() -> stringResource(id = R.string.assetDetails_supplyUnkown)
                    else -> supply.formatted()
                },
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End
            )
        }

        lsu?.fungibleResource?.divisibility?.let {
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
            behaviours = lsu?.fungibleResource?.behaviours,
            onInfoClick = onInfoClick
        )

        TagsSection(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            tags = lsu?.fungibleResource?.tags
        )

        lsu?.resource?.let { resource ->
            NonStandardMetadataSection(resource = resource)
        }
    }
}

@Composable
private fun LSUIconSection(
    modifier: Modifier = Modifier,
    lsu: LiquidStakeUnit?
) {
    if (lsu != null) {
        Thumbnail.LSU(
            modifier = modifier.size(104.dp),
            liquidStakeUnit = lsu
        )
    } else {
        Box(
            modifier = modifier
                .size(104.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun LSUResourceValue(
    modifier: Modifier = Modifier,
    amount: CountedAmount?,
    price: AssetPrice.LSUPrice?,
    isLoadingBalance: Boolean
) {
    Row(
        modifier = modifier
            .assetOutlineBorder()
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.Fungible(
            modifier = Modifier.size(44.dp),
            token = Resource.FungibleResource(
                address = XrdResource.address(Gateway.default.network.id),
                ownedAmount = null
            )
        )
        Text(
            modifier = Modifier.weight(1f),
            text = XrdResource.SYMBOL,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 2
        )

        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(min = RadixTheme.dimensions.amountShimmeringWidth)
                    .radixPlaceholder(visible = amount == null),
                contentAlignment = Alignment.CenterEnd
            ) {
                amount?.let {
                    CountedAmountSection(
                        countedAmount = it,
                        isPredictedAmountCompact = true
                    )
                }
            }

            val xrdPrice = remember(price, amount) {
                (amount as? CountedAmount.Exact)?.amount?.let { amount ->
                    price?.xrdPrice(amount)
                }
            }
            if (isLoadingBalance) {
                ShimmeringView(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingXXSmall)
                        .height(12.dp)
                        .fillMaxWidth(0.3f),
                    isVisible = true
                )
            }
            if (xrdPrice != null) {
                FiatBalanceView(
                    fiatPrice = xrdPrice,
                    textStyle = RadixTheme.typography.body2HighImportance
                )
            }
        }
    }
}

@Composable
fun LiquidStakeUnit?.name(): String = this?.name?.ifEmpty {
    stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)
} ?: stringResource(id = R.string.account_poolUnits_unknownPoolUnitName)

@Composable
@Preview
@UsesSampleValues
private fun LSUDialogContentPreview() {
    RadixWalletPreviewTheme {
        LSUDialogContent(
            lsu = LiquidStakeUnit(
                fungibleResource = Resource.FungibleResource(
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
                validator = Validator(
                    address = ValidatorAddress.sampleMainnet(),
                    totalXrdStake = null
                )
            ),
            price = null,
            args = AssetDialogArgs.Fungible(
                resourceAddress = ResourceAddress.xrd(NetworkId.MAINNET),
                isNewlyCreated = false,
                underAccountAddress = null,
                amounts = mapOf(ResourceAddress.xrd(NetworkId.MAINNET).string to CountedAmount.Exact(Decimal192.sample()))
            ),
            isLoadingBalance = false,
            onInfoClick = {}
        )
    }
}
