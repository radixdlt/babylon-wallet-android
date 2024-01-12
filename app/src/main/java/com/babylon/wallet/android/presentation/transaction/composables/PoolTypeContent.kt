package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PoolTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    previewType: PreviewType.Transfer.Pool
) {
    val poolSectionLabel = when (previewType.actionType) {
        PreviewType.Transfer.Pool.ActionType.Contribution -> "Contributing to pools"
        PreviewType.Transfer.Pool.ActionType.Redemption -> "Redeeming from pools"
    }
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onFungibleResourceClick = onFungibleResourceClick,
        onNonFungibleResourceClick = { _, _, _ -> },
        previewType = previewType,
        onPromptForGuarantees = {},
        middleSection = {
            PoolsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                pools = previewType.pools.toPersistentList(),
                text = poolSectionLabel,
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PoolTypePreview() {
    RadixWalletTheme {
        PoolTypeContent(
            state = TransactionReviewViewModel.State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onFungibleResourceClick = { _, _ -> },
            previewType = PreviewType.Transfer.Pool(
                to = persistentListOf(),
                from = listOf(SampleDataProvider().accountWithTransferablePool).toPersistentList(),
                pools = persistentListOf(),
                actionType = PreviewType.Transfer.Pool.ActionType.Contribution
            ),
        )
    }
}
