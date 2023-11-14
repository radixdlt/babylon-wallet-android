package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.toPersistentList

@Composable
fun TransactionPreviewTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    preview: PreviewType.Transfer,
    onPromptForGuarantees: () -> Unit,
    onDappClick: (DAppWithResources) -> Unit,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column {
            state.message?.let {
                TransactionMessageContent(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    transactionMessage = it
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            WithdrawAccountContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                from = preview.from.toPersistentList(),
                showStrokeLine = preview.from.toPersistentList().isNotEmpty() ||
                    preview.dApps.toPersistentList().isNotEmpty(),
                onFungibleResourceClick = { onFungibleResourceClick(it) },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem)
                }
            )

            ConnectedDAppsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                connectedDApps = preview.dApps.toPersistentList(),
                onDAppClick = onDappClick,
                showStrokeLine = preview.dApps.toPersistentList().isNotEmpty()
            )

            DepositAccountContent(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
                to = preview.to.toPersistentList(),
                promptForGuarantees = onPromptForGuarantees,
                showStrokeLine = false,
                onFungibleResourceClick = { onFungibleResourceClick(it) },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem)
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewTypePreview() {
    RadixWalletTheme {
        TransactionPreviewTypeContent(
            state = TransactionReviewViewModel.State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            preview = PreviewType.Transfer(
                from = emptyList(),
                to = listOf(SampleDataProvider().accountWithTransferableResourcesOwned)
            ),
            onPromptForGuarantees = {},
            onDappClick = { _ -> },
            onFungibleResourceClick = { _ -> },
            onNonFungibleResourceClick = { _, _ -> }
        )
    }
}
