package com.babylon.wallet.android.presentation.history.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.BalanceChange
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.name
import com.babylon.wallet.android.utils.timestampHoursMinutes
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.core.displayableQuantity
import timber.log.Timber

@Composable
fun HistoryTransactionItem(modifier: Modifier = Modifier, transactionItem: TransactionHistoryItem, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectMedium
            ),
    ) {
        Column(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            val withdrawn = remember(transactionItem.withdrawn) {
                transactionItem.withdrawn
            }
            val deposited = remember(transactionItem.deposited) {
                transactionItem.deposited
            }
            if (transactionItem.noBalanceChanges) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TypeAndTimestampLabel(item = transactionItem)
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingMedium),
                    text = "No deposits or withdrawals from this account in this transaction.", // TODO crowdin
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1
                )
            } else {
                val withdrawnShown = withdrawn.isNotEmpty()
                if (withdrawnShown) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                    ) {
                        Icon(painter = painterResource(id = DSR.ic_tx_withdrawn), contentDescription = null, tint = Color.Unspecified)
                        Text(
                            text = "Withdrawn", // TODO crowdin
                            style = RadixTheme.typography.body2Header,
                            color = RadixTheme.colors.gray1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TypeAndTimestampLabel(item = transactionItem)
                    }
                    Column(
                        modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                    ) {
                        val lastItem = withdrawn.last()
                        withdrawn.forEach { withdraw ->
                            val addDivider = lastItem != withdraw
                            BalanceChangeItem(withdraw)
                            if (addDivider) {
                                HorizontalDivider(color = RadixTheme.colors.gray3)
                            }
                        }
                    }
                }
                if (deposited.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                    ) {
                        Icon(painter = painterResource(id = DSR.ic_tx_deposited), contentDescription = null, tint = Color.Unspecified)
                        Text(
                            text = "Deposited", // TODO crowdin
                            style = RadixTheme.typography.body2Header,
                            color = RadixTheme.colors.green1
                        )
                        if (withdrawnShown.not()) {
                            Spacer(modifier = Modifier.weight(1f))
                            TypeAndTimestampLabel(item = transactionItem)
                        }
                    }
                    Column(
                        modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                    ) {
                        val lastItem = deposited.last()
                        deposited.forEach { deposited ->
                            val addDivider = lastItem != deposited
                            BalanceChangeItem(deposited)
                            if (addDivider) {
                                HorizontalDivider(color = RadixTheme.colors.gray3)
                            }
                        }
                    }
                }
            }
        }
        if (transactionItem.unknownTransaction) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.dp)
                    .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectBottomMedium)
                    .padding(RadixTheme.dimensions.paddingMedium),
                text = "This transaction cannot be summarized. Only the raw transaction manifest may be viewed.", // TODO crowding
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
private fun BalanceChangeItem(balanceChange: BalanceChange) {
    when (balanceChange) {
        is BalanceChange.FungibleBalanceChange -> {
            when (val asset = balanceChange.asset) {
                is LiquidStakeUnit -> {
                    LiquidStakeUnitBalanceChange(asset)
                }

                is PoolUnit -> {
                    PoolUnitBalanceChange(asset, balanceChange)
                }

                is Token -> {
                    TokenContent(
                        resource = asset.resource,
                        withdraw = balanceChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingMedium)
                    )
                }

                else -> {}
            }
        }

        is BalanceChange.NonFungibleBalanceChange -> {
            when (val asset = balanceChange.asset) {
                is NonFungibleCollection -> {
                    asset.resource.items.forEachIndexed { _, nftItem ->
                        val addDivider = nftItem != asset.resource.items.last()
                        NftItemBalanceChange(nftItem, asset.resource)
                        if (addDivider) {
                            HorizontalDivider(color = RadixTheme.colors.gray3)
                        }
                    }
                }

                is StakeClaim -> {
                    StakeClaimBalanceChange(asset)
                }

                null -> Timber.d("Unknown asset type")
            }
        }
    }
}

@Composable
private fun StakeClaimBalanceChange(asset: StakeClaim, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(24.dp),
                collection = asset.resource
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = asset.resource.name.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = asset.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.transactionReview_toBeClaimed).uppercase(), // TODO crowdin
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        asset.resource.items.forEachIndexed { index, item ->
            val addSpacer = index != asset.resource.items.lastIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
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
                    text = item.claimAmountXrd?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.body1HighImportance,
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
private fun PoolUnitBalanceChange(
    asset: PoolUnit,
    balanceChange: BalanceChange.FungibleBalanceChange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.PoolUnit(
                modifier = Modifier.size(24.dp),
                poolUnit = asset
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = asset.name(),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val associatedDAppName = remember(asset) {
                    asset.pool?.associatedDApp?.name
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
            Text(
                modifier = Modifier,
                text = balanceChange.balanceChange.abs().displayableQuantity(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
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
        val poolResources = asset.pool?.resources.orEmpty()
        Column(modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)) {
            poolResources.forEachIndexed { index, item ->
                val addDivider = index != poolResources.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingMedium
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                ) {
                    Thumbnail.Fungible(
                        modifier = Modifier.size(18.dp),
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
                        text = asset.resourceRedemptionValue(item)?.displayableQuantity().orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
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
    }
}

@Composable
private fun LiquidStakeUnitBalanceChange(
    asset: LiquidStakeUnit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(24.dp),
                liquidStakeUnit = asset,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = asset.fungibleResource.displayTitle.ifEmpty {
                        stringResource(
                            id = R.string.transactionReview_unknown
                        )
                    },
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = asset.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                .padding(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
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
                text = asset.stakeValue()?.displayableQuantity().orEmpty(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NftItemBalanceChange(
    nftItem: Resource.NonFungibleResource.Item,
    nftResource: Resource.NonFungibleResource,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.NonFungible(
            modifier = Modifier.size(24.dp),
            collection = nftResource
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = nftResource.name.ifEmpty {
                    nftResource.resourceAddress.truncatedHash()
                },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = nftItem.name ?: nftItem.localId.displayable,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TokenContent(
    resource: Resource.FungibleResource,
    withdraw: BalanceChange.FungibleBalanceChange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail.Fungible(token = resource, modifier = Modifier.size(24.dp))
        Text(
            text = resource.displayTitle,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = withdraw.balanceChange.abs().displayableQuantity(),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TypeAndTimestampLabel(modifier: Modifier = Modifier, item: TransactionHistoryItem) {
    val text = buildAnnotatedString {
        append(item.transactionClass.description())
        item.timestamp?.timestampHoursMinutes()?.let {
            append(" \u2022 ")
            append(it)
        }
    }
    Text(
        modifier = modifier,
        text = text,
        style = RadixTheme.typography.body2HighImportance,
        maxLines = 1,
        color = RadixTheme.colors.gray2
    )
}

@Composable
fun TransactionClass?.description(): String {
    return when (this) {
        TransactionClass.General -> stringResource(id = R.string.history_transactionClassGeneral)
        TransactionClass.Transfer -> stringResource(id = R.string.history_transactionClassTransfer)
        TransactionClass.PoolContribution -> stringResource(id = R.string.history_transactionClassContribute)
        TransactionClass.PoolRedemption -> stringResource(id = R.string.history_transactionClassRedeem)
        TransactionClass.ValidatorStake -> stringResource(id = R.string.history_transactionClassStaking)
        TransactionClass.ValidatorUnstake -> stringResource(id = R.string.history_transactionClassUnstaking)
        TransactionClass.ValidatorClaim -> stringResource(id = R.string.history_transactionClassClaim)
        TransactionClass.AccountDespositSettingsUpdate -> stringResource(id = R.string.history_transactionClassAccountSettings)
        else -> stringResource(id = R.string.history_transactionClassOther)
    }
}
