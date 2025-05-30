package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.resources.Resource

@Composable
fun DepositAccountContent(
    modifier: Modifier = Modifier,
    to: ImmutableList<AccountWithTransferables>,
    hiddenResourceIds: PersistentList<ResourceIdentifier>,
    onEditGuaranteesClick: () -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit,
    onTransferableNonFungibleByAmountClick: (asset: Transferable.NonFungibleType, BoundedAmount) -> Unit
) {
    if (to.isNotEmpty()) {
        Column(modifier = modifier) {
            SectionTitle(
                titleRes = R.string.interactionReview_depositsHeading,
                iconRes = DSR.ic_arrow_received_downward
            )

            Column(
                modifier = Modifier
                    .padding(top = RadixTheme.dimensions.paddingSmall)
                    .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                    .background(
                        color = RadixTheme.colors.background,
                        shape = RadixTheme.shapes.roundedRectDefault
                    )
                    .padding(RadixTheme.dimensions.paddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                to.onEachIndexed { index, accountEntry ->
                    val lastItem = index == to.size - 1
                    TransactionAccountCard(
                        account = accountEntry,
                        hiddenResourceIds = hiddenResourceIds,
                        hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_deposit),
                        onTransferableFungibleClick = onTransferableFungibleClick,
                        onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick,
                        onTransferableNonFungibleByAmountClick = onTransferableNonFungibleByAmountClick
                    )

                    if (!lastItem) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    }
                }

                val hasCustomisableGuarantees = remember(to) {
                    to.any { it.hasCustomisableGuarantees() }
                }
                if (hasCustomisableGuarantees) {
                    RadixTextButton(
                        modifier = Modifier
                            .padding(top = RadixTheme.dimensions.paddingXXSmall),
                        text = stringResource(id = R.string.interactionReview_customizeGuaranteesButtonTitle),
                        onClick = onEditGuaranteesClick
                    )
                }
            }
        }
    }
}

@Composable
fun StrokeLine(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    val strokeColor = RadixTheme.colors.divider
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 6.dp.toPx() }
    val lineHeight = with(LocalDensity.current) { height.toPx() }
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeInterval, strokeInterval), 0f)
    Canvas(
        modifier
            .width(1.dp)
            .height(height)
    ) {
        val width = size.width
        drawLine(
            color = strokeColor,
            start = Offset(width, 0f),
            end = Offset(width, lineHeight),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun DepositAccountPreview() {
    RadixWalletTheme {
        DepositAccountContent(
            to = listOf(
                AccountWithTransferables(
                    account = InvolvedAccount.Owned(Account.sampleMainnet()),
                    transferables = emptyList()
                )
            ).toPersistentList(),
            hiddenResourceIds = persistentListOf(),
            onEditGuaranteesClick = {},
            onTransferableFungibleClick = { },
            onTransferableNonFungibleItemClick = { _, _ -> },
            onTransferableNonFungibleByAmountClick = { _, _ -> }
        )
    }
}
