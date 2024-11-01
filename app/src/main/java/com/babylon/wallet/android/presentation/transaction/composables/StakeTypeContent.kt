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
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun StakeTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onNonTransferableFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit,
    previewType: PreviewType.Transfer.Staking,
    onPromptForGuarantees: () -> Unit
) {
    val validatorSectionText = when (previewType.actionType) {
        PreviewType.Transfer.Staking.ActionType.Stake ->
            stringResource(id = R.string.interactionReview_stakingToValidatorsHeading).uppercase()

        PreviewType.Transfer.Staking.ActionType.Unstake ->
            stringResource(id = R.string.interactionReview_unstakingFromValidatorsHeading).uppercase()

        PreviewType.Transfer.Staking.ActionType.ClaimStake ->
            stringResource(id = R.string.interactionReview_claimFromValidatorsHeading).uppercase()
    }
    CommonTransferContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = onNonTransferableFungibleClick,
        previewType = previewType,
        onEditGuaranteesClick = onPromptForGuarantees,
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
                isLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onTransferableFungibleClick = { _ -> },
            onNonTransferableFungibleClick = { _, _ -> },
            previewType = PreviewType.Transfer.Staking(
                to = persistentListOf(),
                from = listOf(
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
                badges = emptyList(),
                validators = emptyList(),
                actionType = PreviewType.Transfer.Staking.ActionType.Stake,
                newlyCreatedGlobalIds = emptyList()
            ),
            onPromptForGuarantees = {},
        )
    }
}
