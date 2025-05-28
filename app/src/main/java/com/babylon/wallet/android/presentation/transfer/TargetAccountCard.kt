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
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.transfer.accounts.RnsDomain
import com.babylon.wallet.android.presentation.transfer.assets.SpendingAssetItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.rnsGradient
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.string
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
                color = RadixTheme.colors.divider,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        Row(
            modifier = Modifier
                .header(
                    targetAccount = targetAccount,
                    onChooseAccountClick = onChooseAccountClick
                )
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
                            tint = RadixTheme.colors.textButton,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.assetTransfer_receivingAccount_chooseAccountButton),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.textButton,
                        )
                    }
                }

                is TargetAccount.Other -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = when (val input = targetAccount.resolvedInput) {
                            is TargetAccount.Other.ResolvedInput.AccountInput -> stringResource(id = R.string.assetTransfer_accountList_externalAccountName)
                            is TargetAccount.Other.ResolvedInput.DomainInput -> input.domain.name
                            null -> ""
                        },
                        style = RadixTheme.typography.body1Header,
                        color = White
                    )
                }

                is TargetAccount.Owned -> {
                    Text(
                        modifier = Modifier.padding(start = RadixTheme.dimensions.paddingMedium),
                        text = targetAccount.account.displayName.value,
                        style = RadixTheme.typography.body1Header,
                        color = White
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            targetAccount.validatedAddress?.let {
                ActionableAddressView(
                    address = Address.Account(it),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = White.copy(alpha = 0.8f),
                    iconColor = White.copy(alpha = 0.8f)
                )
            }

            if (isDeletable) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear",
                        tint = if (targetAccount.validatedAddress != null) White else RadixTheme.colors.textSecondary,
                    )
                }
            }
        }

        HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.divider)

        Column(
            modifier = Modifier
                .clip(RadixTheme.shapes.roundedRectBottomMedium)
                .clickable {
                    onAddAssetsClick()
                }
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .background(
                    color = RadixTheme.colors.backgroundSecondary,
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
                        tint = RadixTheme.colors.iconSecondary,
                        contentDescription = "clear"
                    )
                }
                if (targetAccount.isSignatureRequiredForTransfer(resourceAddress = spendingAsset.resourceAddress)) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXSmall))
                    SpendingAssetWarning(
                        text = stringResource(id = R.string.assetTransfer_extraSignature_label),
                        color = RadixTheme.colors.warning
                    )
                }
                if (!spendingAsset.canDeposit) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXSmall))
                    SpendingAssetWarning(
                        text = stringResource(id = R.string.assetTransfer_depositStatus_denied),
                        color = RadixTheme.colors.error
                    )
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }

            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                style = RadixTheme.typography.body1Header,
                text = " + " + stringResource(id = R.string.assetTransfer_receivingAccount_addAssetsButton),
                color = RadixTheme.colors.textButton
            )
        }
    }
}

@Composable
fun Modifier.header(
    targetAccount: TargetAccount,
    onChooseAccountClick: () -> Unit
): Modifier = when (targetAccount) {
    is TargetAccount.Skeleton ->
        this
            .clip(RadixTheme.shapes.roundedRectTopMedium)
            .background(RadixTheme.colors.background)
            .clickable {
                onChooseAccountClick()
            }

    is TargetAccount.Owned ->
        this
            .background(
                brush = targetAccount.account.appearanceId.gradient(),
                shape = RadixTheme.shapes.roundedRectTopMedium
            )

    is TargetAccount.Other -> {
        when (targetAccount.resolvedInput) {
            is TargetAccount.Other.ResolvedInput.DomainInput -> this
                .rnsGradient(
                    domain = targetAccount.resolvedInput.domain,
                    defaultColor = if (RadixTheme.config.isDarkTheme) {
                        RadixTheme.colors.backgroundTertiary
                    } else {
                        RadixTheme.colors.iconSecondary
                    },
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
            else -> this
                .background(
                    color = if (RadixTheme.config.isDarkTheme) {
                        RadixTheme.colors.backgroundTertiary
                    } else {
                        RadixTheme.colors.iconSecondary
                    },
                    shape = RadixTheme.shapes.roundedRectTopMedium
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

            TargetAccountCard(
                onChooseAccountClick = {},
                onAddAssetsClick = {},
                onRemoveAssetClicked = {},
                onAmountTyped = { _, _ -> },
                onAssetClick = {},
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = with(Account.sampleMainnet.alice) {
                    TargetAccount.Other(
                        typed = address.string,
                        resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(
                            accountAddress = address
                        ),
                        validity = TargetAccount.Other.InputValidity.VALID,
                        id = UUIDGenerator.uuid().toString(),
                        spendingAssets = persistentSetOf()
                    )
                }
            )

            TargetAccountCard(
                onChooseAccountClick = {},
                onAddAssetsClick = {},
                onRemoveAssetClicked = {},
                onAmountTyped = { _, _ -> },
                onAssetClick = {},
                onMaxAmountClicked = {},
                onDeleteClick = {},
                targetAccount = TargetAccount.Other(
                    typed = "bob.xrd",
                    resolvedInput = TargetAccount.Other.ResolvedInput.DomainInput(
                        domain = RnsDomain(
                            accountAddress = Account.sampleMainnet.bob.address,
                            imageUrl = "",
                            name = "bob.xrd"
                        )
                    ),
                    validity = TargetAccount.Other.InputValidity.VALID,
                    id = UUIDGenerator.uuid().toString(),
                    spendingAssets = persistentSetOf()
                )
            )
        }
    }
}
