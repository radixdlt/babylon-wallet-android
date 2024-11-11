package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.WarningText
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
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit,
    onTransferableNonFungibleByAmountClick: (asset: Transferable.NonFungibleType, BoundedAmount) -> Unit
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
            val lastAsset = index == account.transferables.lastIndex && !account.additionalTransferablesPresent
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
                        TransferableNonFungibleItemContent(
                            modifier = Modifier.throttleClickable {
                                onTransferableNonFungibleItemClick(transferable, item)
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

                        if (!lastNFT || transferable.amount.additional != null) {
                            HorizontalDivider(color = RadixTheme.colors.gray4)
                        }
                    }

                    // Show additional amount
                    transferable.amount.additional?.let { amount ->
                        TransferableNonFungibleAmountContent(
                            modifier = Modifier.throttleClickable {
                                onTransferableNonFungibleByAmountClick(transferable, amount)
                            },
                            transferableNFTCollection = transferable,
                            shape = if (lastAsset) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                            amount = amount,
                            isHidden = remember(transferable.resourceAddress, hiddenResourceIds) {
                                transferable.resourceAddress in hiddenResourceIds.nonFungibles()
                            },
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

                is Transferable.NonFungibleType.StakeClaim -> {
                    TransferableStakeClaimNftItemContent(
                        transferableStakeClaim = transferable,
                        shape = if (lastAsset && transferable.amount.additional == null) {
                            RadixTheme.shapes.roundedRectBottomMedium
                        } else {
                            RectangleShape
                        },
                        onClick = onTransferableNonFungibleItemClick
                    )

                    transferable.amount.additional?.let { amount ->
                        HorizontalDivider(color = RadixTheme.colors.gray4)

                        TransferableStakeClaimItemHeader(
                            modifier = Modifier
                                .background(
                                    color = RadixTheme.colors.gray5,
                                    shape = shape
                                )
                                .padding(vertical = RadixTheme.dimensions.paddingMedium)
                                .throttleClickable { onTransferableNonFungibleByAmountClick(transferable, amount) },
                            transferableStakeClaim = transferable,
                            additionalAmount = amount
                        )
                    }
                }
            }

            if (lastAsset.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }

        if (account.additionalTransferablesPresent) {
            UnknownDeposits()
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

@Composable
private fun UnknownDeposits() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectBottomMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        verticalAlignment = CenterVertically,
    ) {
        Image(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.unknown_resources),
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        HorizontalStrokeLine()

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

        WarningText(
            text = AnnotatedString(stringResource(id = R.string.interactionReview_unknown_deposits)),
            textStyle = RadixTheme.typography.body2HighImportance,
            contentColor = RadixTheme.colors.orange1
        )
    }
}

@Composable
private fun HorizontalStrokeLine(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = "----",
        style = RadixTheme.typography.body2HighImportance,
        color = RadixTheme.colors.gray4
    )
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
                        amount = BoundedAmount.Exact(666.toDecimal192()),
                        isNewlyCreated = false
                    )
                )
            ),
            hiddenResourceIds = persistentListOf(),
            hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_withdraw),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleItemClick = { _, _ -> },
            onTransferableNonFungibleByAmountClick = { _, _ -> }
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
                ),
                additionalTransferablesPresent = true
            ),
            hiddenResourceIds = persistentListOf(),
            hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_withdraw),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleItemClick = { _, _ -> },
            onTransferableNonFungibleByAmountClick = { _, _ -> }
        )
    }
}
