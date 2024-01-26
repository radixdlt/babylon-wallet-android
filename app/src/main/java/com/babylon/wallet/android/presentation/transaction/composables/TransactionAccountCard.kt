package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Other
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Owned
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.name
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransactionAccountCardHeader(
            account = account,
            shape = RadixTheme.shapes.roundedRectTopMedium
        )

        val fungibleAmountTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableAsset.Fungible.Token }
        }

        val nftTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableAsset.NonFungible.NFTAssets }
        }

        val lsuTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableAsset.Fungible.LSUAsset }
        }

        val stakeClaimNftTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableAsset.NonFungible.StakeClaimAssets }
        }

        val poolUnitTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableAsset.Fungible.PoolUnitAsset }
        }

        // Fungibles
        fungibleAmountTransferables.forEachIndexed { index, amountTransferable ->
            val lastItem = if (nftTransferables.isEmpty()) index == fungibleAmountTransferables.lastIndex else false
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
            val transferableFungibleAmount = amountTransferable.transferable as TransferableAsset.Fungible.Token

            TransferableItemContent(
                modifier = Modifier.throttleClickable {
                    onFungibleResourceClick(transferableFungibleAmount.resource, transferableFungibleAmount.isNewlyCreated)
                },
                transferable = amountTransferable,
                shape = shape,
            )
            if (lastItem.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }

        // Non fungibles
        nftTransferables.forEachIndexed { collectionIndex, nftTransferable ->
            val collection = nftTransferable.transferable as TransferableAsset.NonFungible.NFTAssets
            // Show each nft item
            collection.resource.items.forEachIndexed { itemIndex, item ->
                val lastItem = itemIndex == collection.resource.items.lastIndex && collectionIndex == nftTransferables.lastIndex
                TransferableNftItemContent(
                    modifier = Modifier.throttleClickable { onNonFungibleResourceClick(collection.resource, item, collection.isNewlyCreated) },
                    transferable = collection,
                    shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                    nftItem = item
                )
            }
        }

        lsuTransferables.forEachIndexed { index, transferable ->
            val lastItem = index == lsuTransferables.lastIndex
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
            val transferableLsu = transferable.transferable as TransferableAsset.Fungible.LSUAsset

            TransferableLsuItemContent(
                modifier = Modifier.throttleClickable {
                    onFungibleResourceClick(transferableLsu.lsu.fungibleResource, transferableLsu.isNewlyCreated)
                },
                transferable = transferable,
                shape = shape,
            )
            if (lastItem.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
        stakeClaimNftTransferables.forEachIndexed { index, transferable ->
            val lastItem = index == stakeClaimNftTransferables.lastIndex
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
            val transferableStakeClaim = transferable.transferable as TransferableAsset.NonFungible.StakeClaimAssets

            TransferableStakeClaimNftItemContent(
                transferable = transferableStakeClaim,
                shape = shape,
                onNonFungibleResourceClick = onNonFungibleResourceClick
            )
            if (lastItem.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }

        poolUnitTransferables.forEachIndexed { index, transferable ->
            val lastItem = index == poolUnitTransferables.lastIndex
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape

            TransferablePoolUnitItemContent(
                transferable = transferable,
                shape = shape,
                onFungibleResourceClick = onFungibleResourceClick
            )
            if (lastItem.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Composable
fun TransactionAccountCardHeader(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    shape: Shape = RadixTheme.shapes.roundedRectTopMedium
) {
    AccountCardHeader(
        displayName = when (account) {
            is Other -> stringResource(id = R.string.transactionReview_externalAccountName)
            is Owned -> account.account.displayName
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = when (account) {
                    is Other -> SolidColor(RadixTheme.colors.gray2)
                    is Owned -> Brush.linearGradient(getAccountGradientColorsFor(account.account.appearanceID))
                },
                shape = shape
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
fun AccountDepositAccountCardHeader(account: Network.Account, modifier: Modifier = Modifier) {
    AccountCardHeader(
        displayName = account.displayName,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(getAccountGradientColorsFor(account.appearanceID)),
                shape = RadixTheme.shapes.roundedRectTopMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
private fun AccountCardHeader(modifier: Modifier = Modifier, displayName: String, address: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            modifier = Modifier.weight(1f, false),
            color = RadixTheme.colors.white
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        ActionableAddressView(
            address = address,
            textStyle = RadixTheme.typography.body2Regular,
            textColor = RadixTheme.colors.white,
            iconColor = RadixTheme.colors.white
        )
    }
}

@Composable
private fun TransferableItemContent(
    modifier: Modifier = Modifier,
    transferable: Transferable,
    shape: Shape
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        when (val resource = transferable.transferable) {
            is TransferableAsset.Fungible.Token -> {
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = resource.resource,
                )
            }

            is TransferableAsset.NonFungible.NFTAssets -> {
                Thumbnail.NonFungible(
                    modifier = Modifier.size(44.dp),
                    collection = resource.resource,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
            }

            else -> {}
        }
        Text(
            modifier = Modifier.weight(1f),
            text = when (val resource = transferable.transferable) {
                is TransferableAsset.Fungible.Token -> resource.resource.displayTitle
                is TransferableAsset.NonFungible.NFTAssets -> resource.resource.name
                else -> ""
            }.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GuaranteesSection(transferable)
    }
}

@Composable
private fun GuaranteesSection(transferable: Transferable, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        val guaranteedQuantity = transferable.guaranteeAssertion as? GuaranteeAssertion.ForAmount
        Row(
            modifier = Modifier,
            verticalAlignment = CenterVertically
        ) {
            if (guaranteedQuantity != null) {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_estimated),
                    style = RadixTheme.typography.body2Link,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            (transferable.transferable as? TransferableAsset.Fungible)?.let {
                Text(
                    modifier = Modifier,
                    text = it.amount.displayableQuantity(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
        guaranteedQuantity?.let { quantity ->
            Row {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.transactionReview_guaranteed),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Text(
                    modifier = Modifier,
                    text = quantity.amount.displayableQuantity(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TransferableLsuItemContent(
    modifier: Modifier = Modifier,
    transferable: Transferable,
    shape: Shape
) {
    val transferableLsu = transferable.transferable as TransferableAsset.Fungible.LSUAsset
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            )
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(44.dp),
                liquidStakeUnit = transferableLsu.lsu,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferableLsu.lsu.fungibleResource.displayTitle.ifEmpty {
                        stringResource(
                            id = R.string.transactionReview_unknown
                        )
                    },
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transferableLsu.lsu.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
//            Icon(painter = painterResource(id = DSR.ic_info_outline), contentDescription = null, tint = RadixTheme.colors.gray3)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.transactionReview_worth).uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = CenterVertically,
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
            Text(
                text = XrdResource.SYMBOL,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
            Text(
                modifier = Modifier.weight(1f),
                text = transferableLsu.xrdWorth.displayableQuantity(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End,
                maxLines = 2
            )
        }
        if (transferable.hasEditableGuarantees) {
            GuaranteesSection(
                transferable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingMedium)
            )
        }
    }
}

@Composable
private fun TransferableStakeClaimNftItemContent(
    modifier: Modifier = Modifier,
    transferable: TransferableAsset.NonFungible.StakeClaimAssets,
    shape: Shape,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            )
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = transferable.resource
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferable.resource.name.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transferable.claim.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
//            Icon(painter = painterResource(id = DSR.ic_info_outline), contentDescription = null, tint = RadixTheme.colors.gray3)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = "To be claimed".uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        transferable.resource.items.forEachIndexed { index, item ->
            val addSpacer = index != transferable.resource.items.lastIndex
            Row(
                modifier = Modifier
                    .clip(RadixTheme.shapes.roundedRectSmall)
                    .throttleClickable {
                        onNonFungibleResourceClick(
                            transferable.resource,
                            item,
                            transferable.isNewlyCreated
                        )
                    }
                    .fillMaxWidth()
                    .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalAlignment = CenterVertically,
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
                Text(
                    text = XrdResource.SYMBOL,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = transferable.xrdWorthPerNftItem[item.localId.displayable]?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
            if (addSpacer) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }
    }
}

@Composable
private fun TransferablePoolUnitItemContent(
    modifier: Modifier = Modifier,
    transferable: Transferable,
    shape: Shape,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit
) {
    val transferablePoolUnit = transferable.transferable as TransferableAsset.Fungible.PoolUnitAsset
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            )
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.PoolUnit(
                modifier = Modifier.size(44.dp),
                poolUnit = transferablePoolUnit.unit
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferablePoolUnit.unit.name(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val associatedDAppName = remember(transferablePoolUnit) {
                    transferablePoolUnit.unit.pool?.associatedDApp?.name
                }
                if (!associatedDAppName.isNullOrEmpty()) {
                    Text(
                        text = associatedDAppName,
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
//            Icon(painter = painterResource(id = DSR.ic_info_outline), contentDescription = null, tint = RadixTheme.colors.gray3)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.transactionReview_worth).uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        val poolResources = transferablePoolUnit.unit.pool?.resources.orEmpty()
        Column(modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)) {
            poolResources.forEachIndexed { index, item ->
                val addDivider = index != poolResources.lastIndex
                Row(
                    modifier = Modifier
                        .clip(RadixTheme.shapes.roundedRectSmall)
                        .throttleClickable {
                            onFungibleResourceClick(
                                item,
                                transferablePoolUnit.isNewlyCreated
                            )
                        }
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                ) {
                    Thumbnail.Fungible(
                        modifier = Modifier.size(44.dp),
                        token = item,
                    )
                    Text(
                        text = item.displayTitle,
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray1,
                        maxLines = 2
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = transferablePoolUnit.contributionPerResource[item.resourceAddress]?.displayableQuantity().orEmpty(),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray1,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )
                }
                if (addDivider) {
                    HorizontalDivider(color = RadixTheme.colors.gray3)
                }
            }
        }
        if (transferable.hasEditableGuarantees) {
            GuaranteesSection(
                transferable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingMedium)
            )
        }
    }
}

@Composable
private fun TransferableNftItemContent(
    modifier: Modifier = Modifier,
    transferable: TransferableAsset.NonFungible.NFTAssets,
    shape: Shape,
    nftItem: Resource.NonFungibleResource.Item
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.NonFungible(
            modifier = Modifier.size(44.dp),
            collection = transferable.resource,
            shape = RadixTheme.shapes.roundedRectSmall
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = nftItem.localId.displayable,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = (nftItem.name ?: transferable.resource.name).ifEmpty {
                    stringResource(id = R.string.transactionReview_unknown)
                },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardPreview() {
    RadixWalletTheme {
        TransactionAccountCard(
            account = Owned(
                account = SampleDataProvider().sampleAccount(),
                resources = SampleDataProvider().sampleFungibleResources().map {
                    Transferable.Withdrawing(
                        transferable = TransferableAsset.Fungible.Token(
                            amount = "689.203".toBigDecimal(),
                            resource = it,
                            isNewlyCreated = false
                        )
                    )
                }
            ),
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> }
        )
    }
}
