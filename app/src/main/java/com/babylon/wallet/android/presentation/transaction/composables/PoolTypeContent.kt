package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PoolTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    previewType: PreviewType.Transfer.Pool,
    onPromptForGuarantees: () -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownPoolsClick: (List<Pool>) -> Unit
) {
    val poolSectionLabel = when (previewType.actionType) {
        PreviewType.Transfer.Pool.ActionType.Contribution -> stringResource(id = R.string.transactionReview_poolContributionHeading)
        PreviewType.Transfer.Pool.ActionType.Redemption -> stringResource(id = R.string.transactionReview_poolRedemptionHeading)
    }
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = { _, _ -> },
        previewType = previewType,
        onPromptForGuarantees = onPromptForGuarantees,
        middleSection = {
            PoolsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                pools = previewType.poolsInvolved.toImmutableList(),
                text = poolSectionLabel,
                onDAppClick = onDAppClick,
                onUnknownPoolComponentsClick = onUnknownPoolsClick
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
            onTransferableFungibleClick = {},
            previewType = PreviewType.Transfer.Pool(
                to = persistentListOf(),
                from = listOf(SampleDataProvider().accountWithTransferablePool).toPersistentList(),
                actionType = PreviewType.Transfer.Pool.ActionType.Contribution
            ),
            onPromptForGuarantees = {},
            onDAppClick = {},
            onUnknownPoolsClick = {}
        )
    }
}
