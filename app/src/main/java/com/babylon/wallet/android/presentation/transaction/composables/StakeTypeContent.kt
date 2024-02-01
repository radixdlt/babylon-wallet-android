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
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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

@Preview(showBackground = true)
@Composable
fun StakeUnstakeTypePreview() {
    RadixWalletTheme {
        StakeTypeContent(
            state = TransactionReviewViewModel.State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onTransferableFungibleClick = { _ -> },
            onNonTransferableFungibleClick = { _, _ -> },
            previewType = PreviewType.Transfer.Staking(
                to = persistentListOf(),
                from = listOf(SampleDataProvider().accountWithTransferableResourceLsu).toPersistentList(),
                validators = persistentListOf(),
                actionType = PreviewType.Transfer.Staking.ActionType.Stake
            ),
            onPromptForGuarantees = {},
        )
    }
}
