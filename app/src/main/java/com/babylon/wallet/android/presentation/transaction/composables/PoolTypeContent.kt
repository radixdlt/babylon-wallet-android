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
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PoolTypePreview() {
    RadixWalletTheme {
        PoolTypeContent(
            state = TransactionReviewViewModel.State(
                request = IncomingMessage.IncomingRequest.TransactionRequest(
                    remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId("b49d643908be5b79b1d233c0b21c1c9dd31a8376ab7caee242af42f6ff1c3bcc"),
                    interactionId = "7294770e-5aec-4e49-ada0-e6a2213fc8c8",
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
            onTransferableFungibleClick = {},
            previewType = PreviewType.Transfer.Pool(
                to = persistentListOf(),
                from = listOf(
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
                actionType = PreviewType.Transfer.Pool.ActionType.Contribution
            ),
            onPromptForGuarantees = {},
            onDAppClick = {},
            onUnknownPoolsClick = {}
        )
    }
}
