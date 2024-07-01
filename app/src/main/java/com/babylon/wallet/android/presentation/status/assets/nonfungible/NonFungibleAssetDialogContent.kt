package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.account.composable.MetadataValueView
import com.babylon.wallet.android.presentation.account.composable.MetadataView
import com.babylon.wallet.android.presentation.status.assets.AssetDialogViewModel
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.DescriptionSection
import com.babylon.wallet.android.presentation.status.assets.NonStandardMetadataSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.WorthXRD
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource

@Suppress("CyclomaticComplexMethod")
@Composable
fun NonFungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    resourceAddress: ResourceAddress,
    localId: NonFungibleLocalId?,
    asset: Asset.NonFungible?,
    price: AssetPrice.StakeClaimPrice?,
    isLoadingBalance: Boolean,
    isNewlyCreated: Boolean = false,
    accountContext: Account? = null,
    claimState: AssetDialogViewModel.State.ClaimState? = null,
    onClaimClick: () -> Unit = {}
) {
    val item = asset?.resource?.items?.firstOrNull()
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (localId != null) {
            if (item?.imageUrl != null) {
                Thumbnail.NFT(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    nft = item,
                    cropped = false
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            } else if (item == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(Thumbnail.NFTAspectRatio)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .radixPlaceholder(
                            visible = true,
                            shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                        )
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            if (!item?.description.isNullOrBlank()) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .fillMaxWidth(),
                    text = item?.description.orEmpty(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.gray4
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            if (item != null) {
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    key = stringResource(id = R.string.assetDetails_NFTDetails_id)
                ) {
                    ActionableAddressView(
                        globalId = item.globalId,
                        isVisitableInDashboard = !isNewlyCreated,
                        textStyle = RadixTheme.typography.body1HighImportance,
                        textColor = RadixTheme.colors.gray1
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                if (!item.name.isNullOrBlank()) {
                    MetadataView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                        key = stringResource(id = R.string.assetDetails_NFTDetails_name)
                    ) {
                        Text(
                            text = item.name.orEmpty(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }

            if (asset is StakeClaim && item?.claimEpoch != null && item.claimAmountXrd != null) {
                ClaimNFTInfo(
                    claimState = claimState,
                    item = item,
                    accountContextMissing = accountContext == null,
                    price = price,
                    isLoadingBalance = isLoadingBalance,
                    onClaimClick = onClaimClick
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            if (!item?.nonStandardMetadata.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.gray4
                )

                item?.nonStandardMetadata?.forEach { metadata ->
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    MetadataView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                        metadata = metadata
                    )
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }

        if (localId != null) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = RadixTheme.colors.gray4
            )
        }
        GrayBackgroundWrapper(contentPadding = PaddingValues(bottom = RadixTheme.dimensions.paddingXXLarge)) {
            if (asset?.resource != null) {
                Thumbnail.NonFungible(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .size(104.dp),
                    collection = asset.resource,
                    radius = CornerSize(RadixTheme.dimensions.paddingMedium)
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .size(104.dp)
                        .radixPlaceholder(
                            visible = true,
                            shape = RadixTheme.shapes.roundedRectMedium
                        )
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            DescriptionSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                description = asset?.resource?.description,
                infoUrl = asset?.resource?.infoUrl
            )

            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                address = Address.Resource(resourceAddress),
                isNewlyCreatedEntity = isNewlyCreated
            )
            if (!asset?.resource?.name.isNullOrBlank() && localId != null) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    key = stringResource(id = R.string.assetDetails_name)
                ) {
                    Text(
                        text = asset?.resource?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            if (!isNewlyCreated) {
                MetadataView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .widthIn(min = RadixTheme.dimensions.paddingXXXXLarge * 2)
                            .radixPlaceholder(visible = asset?.resource?.currentSupply == null),
                        text = when {
                            asset?.resource?.currentSupply != null -> when (asset.resource.currentSupply) {
                                0 -> stringResource(id = R.string.assetDetails_supplyUnkown)
                                else -> asset.resource.currentSupply.toString()
                            }

                            else -> ""
                        },
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End
                    )
                }

                BehavioursSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    behaviours = asset?.resource?.behaviours
                )

                TagsSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    tags = asset?.resource?.tags
                )

                asset?.resource?.let { resource ->
                    NonStandardMetadataSection(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        resource = resource
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimNFTInfo(
    modifier: Modifier = Modifier,
    claimState: AssetDialogViewModel.State.ClaimState?,
    item: Resource.NonFungibleResource.Item,
    accountContextMissing: Boolean,
    price: AssetPrice.StakeClaimPrice? = null,
    isLoadingBalance: Boolean,
    onClaimClick: () -> Unit
) {
    val showClaimButton = claimState is AssetDialogViewModel.State.ClaimState.ReadyToClaim && !accountContextMissing
    Column(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
            .fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            color = RadixTheme.colors.gray4
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = RadixTheme.dimensions.paddingDefault,
                    horizontal = RadixTheme.dimensions.paddingMedium
                ),
            text = stringResource(id = R.string.assetDetails_staking_currentRedeemableValue),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        val fiatPrice = remember(price, item) {
            price?.xrdPrice(item)
        }
        WorthXRD(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
            amount = claimState?.amount,
            fiatPrice = fiatPrice,
            isLoadingBalance = isLoadingBalance,
            iconSize = 44.dp,
            symbolStyle = RadixTheme.typography.body2HighImportance
        )

        when (claimState) {
            is AssetDialogViewModel.State.ClaimState.Unstaking -> {
                Row(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingDefault)
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = stringResource(id = R.string.assetDetails_staking_readyToClaimIn),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray2
                    )

                    Text(
                        text = stringResource(id = R.string.assetDetails_staking_readyToClaimInMinutes, claimState.approximateClaimMinutes),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            is AssetDialogViewModel.State.ClaimState.ReadyToClaim -> {
                if (showClaimButton) {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingDefault)
                            .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.assetDetails_staking_readyToClaim),
                        onClick = onClaimClick
                    )
                }
            }
            null -> {}
        }
    }
}
