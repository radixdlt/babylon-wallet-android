package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Resource

@Composable
fun TransferTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    preview: PreviewType.Transfer.GeneralTransfer,
    onPromptForGuarantees: () -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<String>) -> Unit,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onNonTransferableFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item) -> Unit,
) {
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = onNonTransferableFungibleClick,
        previewType = preview,
        onPromptForGuarantees = onPromptForGuarantees,
        middleSection = {
            ConnectedDAppsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                connectedDApps = preview.dApps.toPersistentList(),
                onDAppClick = onDAppClick,
                onUnknownComponentsClick = onUnknownComponentsClick
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewTypePreview() {
    RadixWalletTheme {
        TransferTypeContent(
            state = TransactionReviewViewModel.State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            preview = PreviewType.Transfer.GeneralTransfer(
                from = emptyList(),
                to = listOf(SampleDataProvider().accountWithTransferableResourcesOwned)
            ),
            onPromptForGuarantees = {},
            onDAppClick = { _ -> },
            onUnknownComponentsClick = {},
            onTransferableFungibleClick = {},
            onNonTransferableFungibleClick = { _, _ -> }
        )
    }
}
