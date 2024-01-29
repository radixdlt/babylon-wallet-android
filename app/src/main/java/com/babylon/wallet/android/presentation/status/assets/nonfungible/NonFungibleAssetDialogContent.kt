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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.presentation.account.composable.AssetMetadataRow
import com.babylon.wallet.android.presentation.account.composable.View
import com.babylon.wallet.android.presentation.status.assets.AssetDialogViewModel
import com.babylon.wallet.android.presentation.status.assets.BehavioursSection
import com.babylon.wallet.android.presentation.status.assets.TagsSection
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.GrayBackgroundWrapper
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.WorthXRD
import com.babylon.wallet.android.presentation.ui.composables.resources.AddressRow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import rdx.works.profile.data.model.pernetwork.Network

@Suppress("CyclomaticComplexMethod")
@Composable
fun NonFungibleAssetDialogContent(
    modifier: Modifier = Modifier,
    resourceAddress: String,
    localId: String?,
    asset: Asset.NonFungible?,
    isNewlyCreated: Boolean = false,
    accountContext: Network.Account? = null,
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
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    nft = item,
                    cropped = false
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            } else if (item == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(Thumbnail.NFTAspectRatio)
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
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
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
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
                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    key = stringResource(id = R.string.assetDetails_NFTDetails_id)
                ) {
                    ActionableAddressView(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingDefault),
                        address = item.globalAddress,
                        textStyle = RadixTheme.typography.body1HighImportance,
                        textColor = RadixTheme.colors.gray1
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            if (asset is StakeClaim && item?.claimEpoch != null && item.claimAmountXrd != null) {
                ClaimNFTInfo(
                    claimState = claimState,
                    accountContextMissing = accountContext == null,
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
                    metadata.View(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
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
        GrayBackgroundWrapper(contentPadding = PaddingValues(bottom = RadixTheme.dimensions.paddingXLarge)) {
            if (asset?.resource != null) {
                Thumbnail.NonFungible(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .size(104.dp),
                    collection = asset.resource,
                    shape = CircleShape
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .size(104.dp)
                        .radixPlaceholder(
                            visible = true,
                            shape = CircleShape
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

            if (!asset?.resource?.description.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    text = asset?.resource?.description.orEmpty(),
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
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            AddressRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                address = resourceAddress
            )
            if (!asset?.resource?.name.isNullOrBlank() && localId != null) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
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
                AssetMetadataRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    key = stringResource(id = R.string.assetDetails_currentSupply)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingDefault)
                            .widthIn(min = RadixTheme.dimensions.paddingXXXLarge * 2)
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
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    behaviours = asset?.resource?.behaviours
                )

                TagsSection(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                    tags = asset?.resource?.tags
                )
            }
        }
    }
}

@Composable
private fun ClaimNFTInfo(
    modifier: Modifier = Modifier,
    claimState: AssetDialogViewModel.State.ClaimState?,
    accountContextMissing: Boolean,
    onClaimClick: () -> Unit
) {
    val showClaimButton = claimState is AssetDialogViewModel.State.ClaimState.ReadyToClaim && !accountContextMissing
    Column(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(bottom = if (showClaimButton) 0.dp else RadixTheme.dimensions.paddingSmall),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = when (claimState) {
                    is AssetDialogViewModel.State.ClaimState.ReadyToClaim -> stringResource(
                        id = if (accountContextMissing) R.string.transactionReview_toBeClaimed else R.string.account_staking_readyToBeClaimed
                    )
                    is AssetDialogViewModel.State.ClaimState.Unstaking ->
                        stringResource(id = R.string.account_staking_unstaking)
                    null -> ""
                }.uppercase(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )

            if (showClaimButton) {
                RadixTextButton(
                    text = stringResource(id = R.string.account_staking_claim),
                    onClick = onClaimClick,
                    textStyle = RadixTheme.typography.body2Link
                )
            }
        }

        WorthXRD(
            amount = claimState?.amount
        )

        if (claimState is AssetDialogViewModel.State.ClaimState.Unstaking) {
            Text(
                modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.assetDetails_staking_unstaking, claimState.approximateClaimMinutes),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
    }
}
