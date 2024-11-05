package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.assets.strokeLine
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.radixdlt.sargon.Address
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Resource

@Composable
fun TransferTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    previewType: PreviewType.Transaction,
    onEditGuaranteesClick: () -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onNonTransferableFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<Address>) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        state.message?.let {
            TransactionMessageContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                transactionMessage = it
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        WithdrawAccountContent(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            from = previewType.from.toPersistentList(),
            hiddenResourceIds = state.hiddenResourceIds,
            onTransferableFungibleClick = onTransferableFungibleClick,
            onNonTransferableFungibleClick = onNonTransferableFungibleClick
        )

        Column(
            modifier = Modifier
                .applyIf(condition = state.showDottedLine, modifier = Modifier.strokeLine())
                .padding(top = RadixTheme.dimensions.paddingLarge)
        ) {
            InvolvedComponentsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                involvedComponents = previewType.involvedComponents,
                onDAppClick = onDAppClick,
                onUnknownComponentsClick = onUnknownComponentsClick
            )

            DepositAccountContent(
                modifier = Modifier
                    .padding(
                        start = RadixTheme.dimensions.paddingDefault,
                        end = RadixTheme.dimensions.paddingDefault
                    )
                    .padding(top = RadixTheme.dimensions.paddingSemiLarge),
                to = previewType.to.toPersistentList(),
                hiddenResourceIds = state.hiddenResourceIds,
                onEditGuaranteesClick = onEditGuaranteesClick,
                onTransferableFungibleClick = onTransferableFungibleClick,
                onNonTransferableFungibleClick = onNonTransferableFungibleClick
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
    }
}
