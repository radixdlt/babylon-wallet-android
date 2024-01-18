package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun TransferTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    preview: PreviewType.Transfer.GeneralTransfer,
    onPromptForGuarantees: () -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownDAppsClick: (ImmutableList<String>) -> Unit,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onFungibleResourceClick = onFungibleResourceClick,
        onNonFungibleResourceClick = onNonFungibleResourceClick,
        previewType = preview,
        onPromptForGuarantees = onPromptForGuarantees,
        middleSection = {
            ConnectedDAppsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                connectedDApps = preview.dApps.toPersistentList(),
                onDAppClick = onDAppClick,
                onUnknownDAppsClick = onUnknownDAppsClick
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
            onUnknownDAppsClick = {},
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> }
        )
    }
}
