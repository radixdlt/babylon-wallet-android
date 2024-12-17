package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.transfer.assets.SpendingAssetItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentSetOf
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TargetAccountCard(
    modifier: Modifier = Modifier,
    onChooseAccountClick: () -> Unit,
    onAddAssetsClick: () -> Unit,
    onRemoveAssetClicked: (SpendingAsset) -> Unit,
    onAmountTyped: (SpendingAsset, String) -> Unit,
    onAssetClick: (SpendingAsset) -> Unit,
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
                        brush = targetAccount.account.appearanceId.gradient(),
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
                    end = RadixTheme.dimensions.paddingXXSmall,
                    top = RadixTheme.dimensions.paddingXXSmall,
                    bottom = RadixTheme.dimensions.paddingXXSmall
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
                            tint = RadixTheme.colors.blue2,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.assetTransfer_receivingAccount_chooseAccountButton),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.blue2,
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
                        text = targetAccount.account.displayName.value,
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.white
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            targetAccount.validatedAddress?.let {
                ActionableAddressView(
                    address = Address.Account(it),
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
                        tint = if (targetAccount.validatedAddress != null) RadixTheme.colors.white else RadixTheme.colors.gray2,
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
                    end = RadixTheme.dimensions.paddingXXSmall
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
                        },
                        onItemClick = {
                            onAssetClick(spendingAsset)
                        }
                    )

                    Icon(
                        modifier = Modifier
                            .padding(
                                start = RadixTheme.dimensions.paddingSmall,
                                end = RadixTheme.dimensions.paddingMedium
                            )
                            .size(20.dp)
                            .clickable {
                                onRemoveAssetClicked(spendingAsset)
                            },
                        imageVector = Icons.Filled.Clear,
                        tint = RadixTheme.colors.gray2,
                        contentDescription = "clear"
                    )
                }
                if (targetAccount.isSignatureRequiredForTransfer(resourceAddress = spendingAsset.resourceAddress)) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXSmall))
                    SpendingAssetWarning(
                        text = stringResource(id = R.string.assetTransfer_extraSignature_label),
                        color = RadixTheme.colors.orange3
                    )
                }
                if (!spendingAsset.canDeposit) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXSmall))
                    SpendingAssetWarning(
                        text = stringResource(id = R.string.assetTransfer_depositStatus_denied),
                        color = RadixTheme.colors.red1
                    )
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                style = RadixTheme.typography.body1Header,
                text = " + " + stringResource(id = R.string.assetTransfer_receivingAccount_addAssetsButton),
                color = RadixTheme.colors.blue2
            )
        }
    }
}

@Composable
fun ColumnScope.SpendingAssetWarning(modifier: Modifier = Modifier, text: String, color: Color) {
    Row(
        modifier = modifier
            .padding(start = RadixTheme.dimensions.paddingXXSmall)
            .align(Alignment.Start)
    ) {
        Icon(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(
                id = DSR.ic_warning_error
            ),
            contentDescription = null,
            tint = color
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = RadixTheme.dimensions.paddingXXSmall),
            style = RadixTheme.typography.body2Regular,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@UsesSampleValues
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
                onAssetClick = {},
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = TargetAccount.Skeleton()
            )

            TargetAccountCard(
                onChooseAccountClick = {},
                onAddAssetsClick = {},
                onRemoveAssetClicked = {},
                onAmountTyped = { _, _ -> },
                onAssetClick = {},
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = TargetAccount.Owned(
                    account = Account.sampleMainnet(),
                    id = UUIDGenerator.uuid().toString(),
                    spendingAssets = persistentSetOf(
                        SpendingAsset.Fungible(
                            resource = Resource.FungibleResource.sampleMainnet()
                        ),
                        with(Resource.NonFungibleResource.sampleMainnet()) {
                            SpendingAsset.NFT(
                                resource = this,
                                item = this.items[0]
                            )
                        }
                    )
                )
            )
        }
    }
}
