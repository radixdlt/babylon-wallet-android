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
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.domain.DApp
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
        PreviewType.Transfer.Pool.ActionType.Contribution -> stringResource(id = R.string.interactionReview_poolContributionHeading)
        PreviewType.Transfer.Pool.ActionType.Redemption -> stringResource(id = R.string.interactionReview_poolRedemptionHeading)
    }
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = { _, _ -> },
        previewType = previewType,
        onEditGuaranteesClick = onPromptForGuarantees,
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
                isLoading = false,
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
                badges = emptyList(),
                actionType = PreviewType.Transfer.Pool.ActionType.Contribution,
                newlyCreatedNFTItems = emptyList()
            ),
            onPromptForGuarantees = {},
            onDAppClick = {},
            onUnknownPoolsClick = {}
        )
    }
}
