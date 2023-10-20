package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.transfer.assets.SpendingAssetItem
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import kotlinx.collections.immutable.persistentSetOf
import rdx.works.core.UUIDGenerator
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
            is TargetAccount.Skeleton -> Modifier
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
                    RadixTextButton(
                        text = stringResource(id = R.string.assetTransfer_receivingAccount_chooseAccountButton),
                        textStyle = RadixTheme.typography.body1Header,
                        contentColor = RadixTheme.colors.gray2,
                        onClick = onChooseAccountClick
                    )
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

        Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

        Column(
            modifier = Modifier
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
            targetAccount.assets.forEach { spendingAsset ->
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

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            RadixTextButton(
                text = stringResource(id = R.string.assetTransfer_receivingAccount_addAssetsButton),
                contentColor = RadixTheme.colors.gray2,
                onClick = onAddAssetsClick
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
                localId = Resource.NonFungibleResource.Item.ID.from("<local_id>"),
                nameMetadataItem = null,
                iconMetadataItem = null
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
                    assets = persistentSetOf(
                        SpendingAsset.Fungible(
                            resource = Resource.FungibleResource(
                                resourceAddress = "resource_rdx_abcd",
                                ownedAmount = BigDecimal.TEN,
                                nameMetadataItem = NameMetadataItem("Radix"),
                                symbolMetadataItem = SymbolMetadataItem(XrdResource.SYMBOL)
                            )
                        ),
                        SpendingAsset.NFT(
                            resource = Resource.NonFungibleResource(
                                resourceAddress = "resource_rdx_abcde",
                                amount = 1L,
                                nameMetadataItem = NameMetadataItem("NFT Collection"),
                                items = listOf(item)
                            ),
                            item = item
                        )
                    )
                )
            )
        }
    }
}
