package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.transfer.assets.SpendingAssetItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.persistentSetOf
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import java.math.BigDecimal

@Composable
fun TargetAccountCard(
    modifier: Modifier = Modifier,
    onChooseAccountClick: () -> Unit,
    onAddAssetsClick: () -> Unit,
    onRemoveAssetClicked: (SpendingAsset) -> Unit,
    onAmountTyped: (SpendingAsset, String) -> Unit,
    onMaxAmountClicked: (SpendingAsset) -> Unit,
    onDeleteClick: () -> Unit,
    isDeletable: Boolean = false,
    targetAccount: TargetAccount,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        val cardModifier = when (targetAccount) {
            is TargetAccount.Skeleton ->
                Modifier
                    .clip(RadixTheme.shapes.roundedRectTopMedium)
                    .clickable {
                        onChooseAccountClick()
                    }

            is TargetAccount.Owned ->
                Modifier
                    .background(
                        brush = Brush.linearGradient(
                            getAccountGradientColorsFor(targetAccount.account.appearanceID)
                        ),
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )

            is TargetAccount.Other ->
                Modifier
                    .background(
                        color = RadixTheme.colors.gray2,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
        }
        Row(
            modifier = cardModifier
                .padding(
                    start = RadixTheme.dimensions.paddingMedium,
                    end = RadixTheme.dimensions.paddingXSmall,
                    top = RadixTheme.dimensions.paddingXSmall,
                    bottom = RadixTheme.dimensions.paddingXSmall
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (targetAccount) {
                is TargetAccount.Skeleton -> {
                    Row(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(id = DSR.ic_entity),
                            tint = RadixTheme.colors.blue1,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.assetTransfer_receivingAccount_chooseAccountButton),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.blue1,
                        )
                    }
                }

                is TargetAccount.Other -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = stringResource(id = R.string.assetTransfer_accountList_externalAccountName),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.white
                    )
                }

                is TargetAccount.Owned -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = targetAccount.account.displayName,
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.white
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (targetAccount.isAddressValid) {
                ActionableAddressView(
                    address = targetAccount.address,
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.white.copy(alpha = 0.8f),
                    iconColor = RadixTheme.colors.white.copy(alpha = 0.8f)
                )
            }

            if (isDeletable) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear",
                        tint = if (targetAccount.isAddressValid) RadixTheme.colors.white else RadixTheme.colors.gray1,
                    )
                }
            }
        }

        HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

        Column(
            modifier = Modifier
                .clip(RadixTheme.shapes.roundedRectBottomMedium)
                .clickable {
                    onAddAssetsClick()
                }
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                .padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingXSmall
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            targetAccount.spendingAssets.forEach { spendingAsset ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpendingAssetItem(
                        modifier = Modifier.weight(1f),
                        asset = spendingAsset,
                        onAmountTyped = {
                            onAmountTyped(spendingAsset, it)
                        },
                        onMaxClicked = {
                            onMaxAmountClicked(spendingAsset)
                        }
                    )

                    IconButton(
                        onClick = {
                            onRemoveAssetClicked(spendingAsset)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            tint = RadixTheme.colors.gray2,
                            contentDescription = "clear"
                        )
                    }
                }
                if (targetAccount.isSignatureRequiredForTransfer(resourceAddress = spendingAsset.address)) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                    Row(
                        modifier = Modifier
                            .padding(start = RadixTheme.dimensions.paddingXSmall)
                            .align(Alignment.Start)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.CenterVertically),
                            painter = painterResource(
                                id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
                            ),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.assetTransfer_extraSignature_label),
                            modifier = Modifier.padding(start = RadixTheme.dimensions.paddingXSmall),
                            style = RadixTheme.typography.body2Regular,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                style = RadixTheme.typography.body1Header,
                text = " + " + stringResource(id = R.string.assetTransfer_receivingAccount_addAssetsButton),
                color = RadixTheme.colors.blue1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TargetAccountCardPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            TargetAccountCard(
                onChooseAccountClick = {},
                onAddAssetsClick = {},
                onRemoveAssetClicked = {},
                onAmountTyped = { _, _ -> },
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = TargetAccount.Skeleton()
            )

            val item = Resource.NonFungibleResource.Item(
                collectionAddress = "resource_rdx_abcde",
                localId = NonFungibleLocalId.sample()
            )
            TargetAccountCard(
                onChooseAccountClick = {},
                onAddAssetsClick = {},
                onRemoveAssetClicked = {},
                onAmountTyped = { _, _ -> },
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = TargetAccount.Owned(
                    account = SampleDataProvider().sampleAccount(),
                    id = UUIDGenerator.uuid().toString(),
                    spendingAssets = persistentSetOf(
                        SpendingAsset.Fungible(
                            resource = Resource.FungibleResource(
                                resourceAddress = "resource_rdx_abcd",
                                ownedAmount = BigDecimal.TEN,
                                metadata = listOf(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.NAME.key,
                                        value = "Radix",
                                        valueType = MetadataType.String
                                    ),
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.SYMBOL.key,
                                        value = XrdResource.SYMBOL,
                                        valueType = MetadataType.String
                                    )
                                )
                            )
                        ),
                        SpendingAsset.NFT(
                            resource = Resource.NonFungibleResource(
                                resourceAddress = "resource_rdx_abcde",
                                amount = 1L,
                                items = listOf(item),
                                metadata = listOf(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.NAME.key,
                                        value = "NFT Collection",
                                        valueType = MetadataType.String
                                    ),
                                )
                            ),
                            item = item
                        )
                    )
                )
            )
        }
    }
}
