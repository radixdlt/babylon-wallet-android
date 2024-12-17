package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.radixdlt.sargon.ResourceIdentifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import rdx.works.core.domain.resources.Resource

@Composable
fun WithdrawAccountContent(
    modifier: Modifier = Modifier,
    from: ImmutableList<AccountWithTransferables>,
    hiddenResourceIds: PersistentList<ResourceIdentifier>,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onTransferableNonFungibleByAmountClick: (asset: Transferable.NonFungibleType, BoundedAmount) -> Unit
) {
    if (from.isNotEmpty()) {
        SectionTitle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RadixTheme.dimensions.paddingDefault)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            titleRes = R.string.interactionReview_withdrawalsHeading,
            iconRes = DSR.ic_arrow_up_right,
        )
        Column(
            modifier = modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = Color.White,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium)
        ) {
            from.forEachIndexed { index, account ->
                TransactionAccountCard(
                    account = account,
                    hiddenResourceIds = hiddenResourceIds,
                    hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_withdraw),
                    onTransferableFungibleClick = onTransferableFungibleClick,
                    onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick,
                    onTransferableNonFungibleByAmountClick = onTransferableNonFungibleByAmountClick
                )

                if (index != from.lastIndex) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
        }
    }
}
