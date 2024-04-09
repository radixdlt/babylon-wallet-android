package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.constraintlayout.compose.ConstraintLayout
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.BalanceChange
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.name
import com.babylon.wallet.android.utils.timestampHoursMinutes
import com.radixdlt.sargon.extensions.abs
import com.radixdlt.sargon.extensions.formatted
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import timber.log.Timber

@Suppress("CyclomaticComplexMethod")
@Composable
fun TransactionHistoryItem(modifier: Modifier = Modifier, transactionItem: TransactionHistoryItem, onClick: () -> Unit) {
    ConstraintLayout(
        modifier
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        val (content, label, message) = createRefs()
        if (!transactionItem.message.isNullOrEmpty()) {
            HistoryMessageContent(
                transactionItem.message,
                Modifier.constrainAs(message) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
        Column(
            Modifier.constrainAs(content) {
                if (transactionItem.message != null) {
                    top.linkTo(message.bottom)
                } else {
                    top.linkTo(parent.top)
                }
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }
        ) {
            Column(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                val borderModifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectMedium)
                    .padding(RadixTheme.dimensions.paddingMedium)
                val isAccountDepositSettingsUpdate = transactionItem.transactionClass == TransactionClass.AccountDespositSettingsUpdate
                val withdrawn = remember(transactionItem.withdrawn) {
                    transactionItem.withdrawn
                }
                val deposited = remember(transactionItem.deposited) {
                    transactionItem.deposited
                }
                if (transactionItem.isFailedTransaction) {
                    LabelSection(text = stringResource(id = R.string.empty))
                    FailedTransactionWarning(borderModifier)
                } else {
                    if (isAccountDepositSettingsUpdate) {
                        LabelSection(
                            text = stringResource(id = R.string.transactionHistory_settingsSection),
                            iconResource = DSR.ic_tx_account_settings
                        )
                        Text(
                            modifier = borderModifier,
                            text = stringResource(id = R.string.transactionHistory_updatedDepositSettings),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray1
                        )
                    }
                    if (transactionItem.hasNoBalanceChanges && isAccountDepositSettingsUpdate.not()) {
                        LabelSection(text = stringResource(id = R.string.empty))
                        Text(
                            modifier = borderModifier,
                            text = stringResource(id = R.string.transactionHistory_noBalanceChanges),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray1
                        )
                    } else {
                        if (withdrawn.isNotEmpty()) {
                            LabelSection(
                                text = stringResource(id = R.string.transactionHistory_withdrawnSection),
                                iconResource = DSR.ic_tx_withdrawn
                            )
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
                            LabelSection(
                                text = stringResource(id = R.string.transactionHistory_depositedSection),
                                iconResource = DSR.ic_tx_deposited,
                                textColor = RadixTheme.colors.green1
                            )
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
            }
        }
        val paddingMedium = RadixTheme.dimensions.paddingMedium
        val paddingDefault = RadixTheme.dimensions.paddingDefault
        TypeAndTimestampLabel(
            Modifier.constrainAs(label) {
                if (transactionItem.message != null) {
                    top.linkTo(message.bottom, paddingMedium)
                } else {
                    top.linkTo(parent.top, paddingMedium)
                }
                end.linkTo(parent.end, paddingDefault)
            },
            item = transactionItem
        )
    }
}

@Composable
private fun FailedTransactionWarning(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(painter = painterResource(id = DSR.ic_warning_error), contentDescription = null, tint = RadixTheme.colors.red1)
        Text(
            text = stringResource(id = R.string.transactionHistory_failedTransaction),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.red1
        )
    }
}

@Composable
private fun LabelSection(
    text: String,
    modifier: Modifier = Modifier,
    iconResource: Int? = null,
    textColor: Color = RadixTheme.colors.gray1
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        iconResource?.let {
            Icon(painter = painterResource(id = it), contentDescription = null, tint = Color.Unspecified)
        }
        Text(
            text = text,
            style = RadixTheme.typography.body2Header,
            color = textColor
        )
    }
}

@Composable
private fun BalanceChangeItem(balanceChange: BalanceChange) {
    when (balanceChange) {
        is BalanceChange.FungibleBalanceChange -> {
            when (val asset = balanceChange.asset) {
                is LiquidStakeUnit -> {
                    LiquidStakeUnitBalanceChange(asset, balanceChange)
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
            .padding(vertical = RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium)) {
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
                        text = asset.resource.name.ifEmpty { stringResource(id = R.string.account_staking_worth) },
                        style = RadixTheme.typography.body2HighImportance,
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
                    style = RadixTheme.typography.body2HighImportance,
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
                text = balanceChange.balanceChange.abs().formatted(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun LiquidStakeUnitBalanceChange(
    asset: LiquidStakeUnit,
    balanceChange: BalanceChange.FungibleBalanceChange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = RadixTheme.dimensions.paddingMedium)
    ) {
        Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium)) {
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
                                id = R.string.account_poolUnits_unknownPoolUnitName
                            )
                        },
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray1,
                        maxLines = 2,
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
                Text(
                    text = balanceChange.balanceChange.abs().formatted(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                    nftResource.address.formatted()
                },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = nftItem.name ?: nftItem.localId.formatted(),
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
            text = withdraw.balanceChange.abs().formatted(),
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
            append(" \u2022 $it")
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
        TransactionClass.General -> stringResource(id = R.string.transactionHistory_manifestClass_General)
        TransactionClass.Transfer -> stringResource(id = R.string.transactionHistory_manifestClass_Transfer)
        TransactionClass.PoolContribution -> stringResource(id = R.string.transactionHistory_manifestClass_Contribute)
        TransactionClass.PoolRedemption -> stringResource(id = R.string.transactionHistory_manifestClass_Redeem)
        TransactionClass.ValidatorStake -> stringResource(id = R.string.transactionHistory_manifestClass_Staking)
        TransactionClass.ValidatorUnstake -> stringResource(id = R.string.transactionHistory_manifestClass_Unstaking)
        TransactionClass.ValidatorClaim -> stringResource(id = R.string.transactionHistory_manifestClass_Claim)
        TransactionClass.AccountDespositSettingsUpdate -> stringResource(id = R.string.transactionHistory_manifestClass_AccountSettings)
        else -> stringResource(id = R.string.transactionHistory_manifestClass_Other)
    }
}
