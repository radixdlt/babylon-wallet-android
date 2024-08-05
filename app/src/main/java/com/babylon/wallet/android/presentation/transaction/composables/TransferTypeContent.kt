package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet
import java.util.UUID

@Composable
fun TransferTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    preview: PreviewType.Transfer.GeneralTransfer,
    onPromptForGuarantees: () -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownComponentsClick: (List<ComponentAddress>) -> Unit,
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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun TransactionPreviewTypePreview() {
    RadixWalletTheme {
        TransferTypeContent(
            state = TransactionReviewViewModel.State(
                request = IncomingMessage.IncomingRequest.TransactionRequest(
                    remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId(
                        "b49d643908be5b79b1d233c0b21c1c9dd31a8376ab7caee242af42f6ff1c3bcc"
                    ),
                    interactionId = UUID.randomUUID().toString(),
                    transactionManifestData = TransactionManifestData(
                        instructions = "CREATE_FUNGIBLE_RESOURCE_WITH_INITIAL_SUPPLY",
                        networkId = NetworkId.MAINNET,
                        message = TransactionManifestData.TransactionMessage.Public("Hello"),
                        version = TransactionVersion.Default.value
                    ),
                    requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata.internal(NetworkId.MAINNET)
                ),
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            preview = PreviewType.Transfer.GeneralTransfer(
                from = emptyList(),
                to = listOf(
                    AccountWithTransferableResources.Owned(
                        account = Account.sampleMainnet(),
                        resources = listOf(
                            Transferable.Depositing(
                                transferable = TransferableAsset.Fungible.Token(
                                    amount = 69.toDecimal192(),
                                    resource = Resource.FungibleResource.sampleMainnet(),
                                    isNewlyCreated = true
                                )
                            )
                        )
                    )
                ),
                newlyCreatedNFTItems = emptyList()
            ),
            onPromptForGuarantees = {},
            onDAppClick = { _ -> },
            onUnknownComponentsClick = {},
            onTransferableFungibleClick = {},
            onNonTransferableFungibleClick = { _, _ -> }
        )
    }
}
