package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransferTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    preview: PreviewType.Transfer.GeneralTransfer,
    onEditGuaranteesClick: () -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<ManifestEncounteredComponentAddress>) -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onNonTransferableFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit,
) {
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = onNonTransferableFungibleClick,
        previewType = preview,
        onEditGuaranteesClick = onEditGuaranteesClick,
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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun TransactionPreviewTypePreview() {
    RadixWalletTheme {
        TransferTypeContent(
            state = TransactionReviewViewModel.State(
                isLoading = false,
                previewType = PreviewType.NonConforming
            ),
            preview = PreviewType.Transfer.GeneralTransfer(
                from = emptyList(),
                to = listOf(
                    AccountWithTransferables.Owned(
                        account = Account.sampleMainnet(),
                        transferables = listOf(
                            Transferable.FungibleType.Token(
                                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                amount = FungibleAmount.Exact("745".toDecimal192()),
                                isNewlyCreated = false
                            )
                        )
                    )
                ),
                newlyCreatedNFTItems = emptyList()
            ),
            onEditGuaranteesClick = {},
            onDAppClick = { _ -> },
            onUnknownComponentsClick = {},
            onTransferableFungibleClick = {},
            onNonTransferableFungibleClick = { _, _ -> }
        )
    }
}
