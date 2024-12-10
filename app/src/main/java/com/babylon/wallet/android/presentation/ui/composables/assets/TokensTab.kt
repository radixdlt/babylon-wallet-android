package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleRandom
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

fun LazyListScope.tokensTab(
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
) {
    if (assetsViewData.isTokensEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Tokens,
                onInfoClick = onInfoClick
            )
        }
    }

    item {
        if (assetsViewData.xrd != null) {
            AssetCard(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge)
            ) {
                TokenItem(
                    token = assetsViewData.xrd,
                    fiatPrice = assetsViewData.prices?.get(assetsViewData.xrd)?.price,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )
            }
        }
    }

    itemsIndexed(
        items = assetsViewData.nonXrdTokens,
        key = { _, token -> token.resource.address.string },
        itemContent = { index, token ->
            AssetCard(
                modifier = Modifier
                    .padding(top = if (index == 0) RadixTheme.dimensions.paddingSemiLarge else 0.dp)
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                itemIndex = index,
                allItemsSize = assetsViewData.nonXrdTokens.size
            ) {
                TokenItem(
                    token = token,
                    fiatPrice = assetsViewData.prices?.get(token)?.price,
                    isLoadingBalance = isLoadingBalance,
                    action = action
                )

                if (index != assetsViewData.nonXrdTokens.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = RadixTheme.colors.gray4
                    )
                }
            }
        }
    )
}

@Composable
private fun TokenItem(
    token: Token,
    fiatPrice: FiatPrice?,
    isLoadingBalance: Boolean,
    modifier: Modifier = Modifier,
    action: AssetsViewAction
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .throttleClickable {
                when (action) {
                    is AssetsViewAction.Click -> {
                        action.onFungibleClick(token.resource)
                    }

                    is AssetsViewAction.Selection -> {
                        action.onFungibleCheckChanged(token.resource, !action.isSelected(token.resource.address))
                    }
                }
            }
            .fillMaxWidth()
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
    ) {
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        Thumbnail.Fungible(
            modifier = Modifier.size(44.dp),
            token = token.resource
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.weight(1f),
            text = token.displayTitle(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        token.resource.ownedAmount?.let { amount ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount.formatted(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                if (isLoadingBalance) {
                    ShimmeringView(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXXXSmall)
                            .height(12.dp)
                            .fillMaxWidth(0.3f),
                        isVisible = true
                    )
                } else if (fiatPrice != null) {
                    FiatBalanceView(fiatPrice = fiatPrice)
                }
            }
        }

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(token.resource, action) {
                action.isSelected(token.resource.address)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onFungibleCheckChanged(token.resource, isChecked)
                }
            )
        } else {
            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        }
    }
}

@UsesSampleValues
internal val previewXrdToken = Token(
    Resource.FungibleResource(
        ResourceAddress.sampleMainnet(),
        Decimal192.sample(),
        metadata = listOf(
            Metadata.Primitive(
                ExplicitMetadataKey.NAME.key,
                value = "XRD",
                valueType = MetadataType.String
            )
        )
    )
)

@Suppress("MagicNumber")
@UsesSampleValues
internal val previewAssetViewData = AssetsViewData(
    xrd = previewXrdToken,
    nonXrdTokens = listOf(
        Token(
            Resource.FungibleResource(
                ResourceAddress.sampleRandom(NetworkId.MAINNET),
                Decimal192.sample(),
                metadata = listOf(
                    Metadata.Primitive(
                        ExplicitMetadataKey.NAME.key,
                        value = "Token 1",
                        valueType = MetadataType.String
                    )
                )
            )
        ),
        Token(
            Resource.FungibleResource(
                ResourceAddress.sampleRandom(NetworkId.MAINNET),
                Decimal192.sample(),
                metadata = listOf(
                    Metadata.Primitive(
                        ExplicitMetadataKey.NAME.key,
                        value = "Token 2",
                        valueType = MetadataType.String
                    )
                )
            )
        )

    ),
    nonFungibleCollections = listOf(
        NonFungibleCollection(
            Resource.NonFungibleResource(
                address = ResourceAddress.sampleMainnet(),
                amount = 3L,
                items = listOf(
                    Resource.NonFungibleResource.Item(
                        collectionAddress = ResourceAddress.sampleMainnet(),
                        localId = NonFungibleLocalId.sample(),
                        metadata = listOf(
                            Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Item 1", MetadataType.String),
                            Metadata.Primitive(
                                key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                                "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/KL%20Haze-medium.jpg",
                                valueType = MetadataType.Url
                            )
                        )
                    ),
                    Resource.NonFungibleResource.Item(
                        collectionAddress = ResourceAddress.sampleMainnet(),
                        localId = NonFungibleLocalId.sample(),
                        metadata = listOf(Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Item 2", MetadataType.String))
                    )
                ),
                metadata = listOf(Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Collection Name", MetadataType.String))
            )
        )
    ),
    epoch = null,
    prices = mapOf(previewXrdToken to AssetPrice.TokenPrice(previewXrdToken, FiatPrice(Decimal192.sample(), SupportedCurrency.USD))),
    poolUnits = listOf(
        PoolUnit(
            stake = Resource.FungibleResource(ResourceAddress.sampleMainnet(), ownedAmount = Decimal192.sample()),
            pool = Pool(
                address = PoolAddress.sampleMainnet(),
                resources = listOf(
                    Resource.FungibleResource(
                        address = ResourceAddress.sampleRandom(NetworkId.MAINNET),
                        ownedAmount = Decimal192.sample()
                    )
                ),
                metadata = listOf(Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Pool 1", MetadataType.String))
            )
        ),
        PoolUnit(
            stake = Resource.FungibleResource(ResourceAddress.sampleMainnet(), ownedAmount = Decimal192.sample()),
            pool = Pool(
                address = PoolAddress.sampleMainnet(),
                resources = listOf(
                    Resource.FungibleResource(
                        address = ResourceAddress.sampleRandom(NetworkId.MAINNET),
                        ownedAmount = Decimal192.sample()
                    )
                ),
                metadata = listOf(Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Pool 2", MetadataType.String))
            )
        )
    ),
    validatorsWithStakes = listOf(
        ValidatorWithStakes(
            validator = Validator(
                address = ValidatorAddress.sampleMainnet(),
                totalXrdStake = Decimal192.sample(),
                stakeUnitResourceAddress = ResourceAddress.sampleMainnet(),
                claimTokenResourceAddress = ResourceAddress.sampleMainnet(),
                metadata = listOf(
                    Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Validator 1", MetadataType.String)
                )
            )
        ),
        ValidatorWithStakes(
            validator = Validator(
                ValidatorAddress.sampleRandom(NetworkId.MAINNET),
                Decimal192.sample(),
                ResourceAddress.sampleMainnet(),
                ResourceAddress.sampleMainnet(),
                metadata = listOf(
                    Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Validator 2", MetadataType.String)
                )
            )
        )
    )
)

@Preview(showBackground = true)
@UsesSampleValues
@Composable
private fun TokensTabPreview() {
    RadixWalletTheme {
        LazyColumn {
            tokensTab(
                assetsViewData = previewAssetViewData,
                isLoadingBalance = false,
                action = AssetsViewAction.Click(
                    onFungibleClick = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onNextNFtsPageRequest = {},
                    onClaimClick = {},
                    onStakesRequest = {},
                    onCollectionClick = {},
                    onTabClick = {}
                ),
                onInfoClick = {}
            )
        }
    }
}
