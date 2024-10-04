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
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.messages.IncomingMessage.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet
import java.util.UUID

@Composable
fun StakeTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onNonTransferableFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item) -> Unit,
    previewType: PreviewType.Transfer.Staking,
    onPromptForGuarantees: () -> Unit
) {
    val validatorSectionText = when (previewType.actionType) {
        PreviewType.Transfer.Staking.ActionType.Stake ->
            stringResource(id = R.string.transactionReview_stakingToValidatorsHeading).uppercase()

        PreviewType.Transfer.Staking.ActionType.Unstake ->
            stringResource(id = R.string.transactionReview_unstakingFromValidatorsHeading).uppercase()

        PreviewType.Transfer.Staking.ActionType.ClaimStake ->
            stringResource(id = R.string.transactionReview_claimFromValidatorsHeading).uppercase()
    }
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = onNonTransferableFungibleClick,
        previewType = previewType,
        onPromptForGuarantees = onPromptForGuarantees,
        middleSection = {
            ValidatorsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                validators = previewType.validators.toPersistentList(),
                text = validatorSectionText,
            )
        }
    )
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun StakeUnstakeTypePreview() {
    RadixWalletTheme {
        StakeTypeContent(
            state = TransactionReviewViewModel.State(
                request = TransactionRequest(
                    remoteEntityId = RemoteEntityID.ConnectorId(
                        "b49d643908be5b79b1d233c0b21c1c9dd31a8376ab7caee242af42f6ff1c3bcc"
                    ),
                    interactionId = UUID.randomUUID().toString(),
                    transactionManifestData = TransactionManifestData(
                        instructions = "CREATE_FUNGIBLE_RESOURCE_WITH_INITIAL_SUPPLY",
                        networkId = NetworkId.MAINNET,
                        message = TransactionManifestData.TransactionMessage.Public("Hello"),
                        version = TransactionVersion.Default.value
                    ),
                    requestMetadata = DappToWalletInteraction.RequestMetadata.internal(NetworkId.MAINNET)
                ),
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onTransferableFungibleClick = { _ -> },
            onNonTransferableFungibleClick = { _, _ -> },
            previewType = PreviewType.Transfer.Staking(
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
                badges = emptyList(),
                validators = emptyList(),
                actionType = PreviewType.Transfer.Staking.ActionType.Stake,
                newlyCreatedNFTItems = emptyList()
            ),
            onPromptForGuarantees = {},
        )
    }
}
