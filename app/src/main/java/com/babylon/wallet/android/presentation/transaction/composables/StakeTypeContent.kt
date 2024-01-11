package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun StakeTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
    previewType: PreviewType.Staking
) {
    val validatorSectionText = when (previewType.actionType) {
        PreviewType.Staking.ActionType.Stake -> stringResource(id = R.string.transactionReview_validators_stake).uppercase()
        PreviewType.Staking.ActionType.Unstake -> stringResource(id = R.string.transactionReview_validators_unstake).uppercase()
        PreviewType.Staking.ActionType.ClaimStake -> stringResource(id = R.string.transactionReview_validators_claim).uppercase()
    }
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
                from = previewType.from.toPersistentList(),
                onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                    onFungibleResourceClick(fungibleResource, isNewlyCreated)
                },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                }
            )

            ValidatorsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                validators = previewType.validators.toPersistentList(),
                text = validatorSectionText,
            )

            DepositAccountContent(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
                to = previewType.to.toPersistentList(),
                promptForGuarantees = {},
                showStrokeLine = true,
                onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                    onFungibleResourceClick(fungibleResource, isNewlyCreated)
                },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
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
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> },
            previewType = PreviewType.Staking(
                to = persistentListOf(),
                from = listOf(SampleDataProvider().accountWithTransferableResourceLsu).toPersistentList(),
                validators = persistentListOf(),
                actionType = PreviewType.Staking.ActionType.Stake
            ),
        )
    }
}
