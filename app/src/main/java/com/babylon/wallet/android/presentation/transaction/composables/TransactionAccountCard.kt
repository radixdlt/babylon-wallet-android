package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.sargon.fungibles
import rdx.works.core.sargon.nonFungibles
import rdx.works.core.sargon.pools

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferables,
    hiddenResourceIds: PersistentList<ResourceIdentifier>,
    hiddenResourceWarning: String,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransactionAccountCardHeader(
            accountWithTransferables = account,
            shape = RadixTheme.shapes.roundedRectTopMedium
        )

        account.transferables.forEachIndexed { index, transferable ->
            val lastAsset = index == account.transferables.lastIndex
            val shape = if (lastAsset) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape

            when (transferable) {
                is Transferable.FungibleType.Token -> TransferableTokenItemContent(
                    modifier = Modifier.throttleClickable {
                        onTransferableFungibleClick(transferable)
                    },
                    transferableToken = transferable,
                    shape = shape,
                    isHidden = remember(
                        transferable,
                        hiddenResourceIds
                    ) { transferable.resourceAddress in hiddenResourceIds.fungibles() },
                    hiddenResourceWarning = hiddenResourceWarning
                )

                is Transferable.NonFungibleType.NFTCollection -> {
                    // Show each nft item
                    transferable.asset.resource.items.forEachIndexed { itemIndex, item ->
                        val lastNFT = itemIndex == transferable.asset.resource.items.lastIndex
                        TransferableNftItemContent(
                            modifier = Modifier.throttleClickable {
                                onTransferableNonFungibleClick(transferable, item)
                            },
                            transferableNFTCollection = transferable,
                            shape = if (lastAsset && lastNFT) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                            nftItem = item,
                            isHidden = remember(
                                item,
                                hiddenResourceIds
                            ) { item.collectionAddress in hiddenResourceIds.nonFungibles() },
                            hiddenResourceWarning = hiddenResourceWarning
                        )
                    }
                }

                is Transferable.FungibleType.LSU -> TransferableLsuItemContent(
                    modifier = Modifier.throttleClickable {
                        onTransferableFungibleClick(transferable)
                    },
                    transferableLSU = transferable,
                    shape = shape,
                )

                is Transferable.FungibleType.PoolUnit -> TransferablePoolUnitItemContent(
                    transferablePoolUnit = transferable,
                    shape = shape,
                    isHidden = remember(
                        transferable,
                        hiddenResourceIds
                    ) { transferable.asset.resource.poolAddress in hiddenResourceIds.pools() },
                    hiddenResourceWarning = hiddenResourceWarning,
                    onClick = onTransferableFungibleClick
                )

                is Transferable.NonFungibleType.StakeClaim -> TransferableStakeClaimNftItemContent(
                    transferableStakeClaim = transferable,
                    shape = shape,
                    onClick = onTransferableNonFungibleClick
                )
            }

            if (lastAsset.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Composable
fun TransactionAccountCardHeader(
    modifier: Modifier = Modifier,
    accountWithTransferables: AccountWithTransferables,
    shape: Shape = RadixTheme.shapes.roundedRectTopMedium
) {
    AccountCardHeader(
        displayName = when (val involvedAccount = accountWithTransferables.account) {
            is InvolvedAccount.Other -> stringResource(id = R.string.interactionReview_externalAccountName)
            is InvolvedAccount.Owned -> involvedAccount.account.displayName.value
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = when (val involvedAccount = accountWithTransferables.account) {
                    is InvolvedAccount.Other -> SolidColor(RadixTheme.colors.gray2)
                    is InvolvedAccount.Owned -> involvedAccount.account.appearanceId.gradient()
                },
                shape = shape
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = accountWithTransferables.account.address
    )
}

@Composable
fun AccountDepositAccountCardHeader(account: Account, modifier: Modifier = Modifier) {
    AccountCardHeader(
        displayName = account.displayName.value,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = account.appearanceId.gradient(),
                shape = RadixTheme.shapes.roundedRectTopMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
private fun AccountCardHeader(modifier: Modifier = Modifier, displayName: String, address: AccountAddress) {
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
            address = Address.Account(address),
            textStyle = RadixTheme.typography.body2Regular,
            textColor = RadixTheme.colors.white,
            iconColor = RadixTheme.colors.white
        )
    }
}

@UsesSampleValues
@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardWithTokenPreview() {
    RadixWalletTheme {
        TransactionAccountCard(
            account = AccountWithTransferables(
                account = InvolvedAccount.Owned(Account.sampleMainnet()),
                transferables = listOf(
                    Transferable.FungibleType.Token(
                        asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                        amount = CountedAmount.Exact(666.toDecimal192()),
                        isNewlyCreated = false
                    )
                )
            ),
            hiddenResourceIds = persistentListOf(),
            hiddenResourceWarning = stringResource(id = R.string.transactionReview_hiddenAsset_withdraw),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleClick = { _, _ -> }
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardWithNFTPreview() {
    RadixWalletTheme {
        val asset = remember {
            NonFungibleCollection(collection = Resource.NonFungibleResource.sampleMainnet())
        }
        val amount = remember(asset) {
            NonFungibleAmount(certain = asset.collection.items)
        }
        TransactionAccountCard(
            account = AccountWithTransferables(
                account = InvolvedAccount.Owned(Account.sampleMainnet()),
                transferables = listOf(
                    Transferable.NonFungibleType.NFTCollection(
                        asset = asset,
                        amount = amount,
                        isNewlyCreated = false
                    )
                )
            ),
            hiddenResourceIds = persistentListOf(),
            hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_withdraw),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleClick = { _, _ -> }
        )
    }
}
