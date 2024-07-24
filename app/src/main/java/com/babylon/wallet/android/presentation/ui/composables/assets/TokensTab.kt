package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.ShimmeringView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

fun LazyListScope.tokensTab(
    assetsViewData: AssetsViewData,
    isLoadingBalance: Boolean,
    action: AssetsViewAction
) {
    if (assetsViewData.isTokensEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Tokens
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
                            .padding(top = RadixTheme.dimensions.paddingXXSmall)
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

@Preview
@UsesSampleValues
@Composable
private fun TokenCardPreview() {
    RadixWalletTheme {
        AssetCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            TokenItem(
                token = Token(
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
                ),
                fiatPrice = FiatPrice(Decimal192.sample(), SupportedCurrency.USD),
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
                )
            )
        }
    }
}
