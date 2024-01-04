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
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun StakeUnstakeTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
    toAccounts: PersistentList<AccountWithTransferableResources>,
    fromAccounts: PersistentList<AccountWithTransferableResources>,
    validators: PersistentList<ValidatorDetail>,
    validatorSectionTitleText: String
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
                from = fromAccounts,
                onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                    onFungibleResourceClick(fungibleResource, isNewlyCreated)
                },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                }
            )

            ValidatorsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                validators = validators,
                text = validatorSectionTitleText,
            )

            DepositAccountContent(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
                to = toAccounts,
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
        StakeUnstakeTypeContent(
            state = TransactionReviewViewModel.State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> },
            toAccounts = persistentListOf(),
            fromAccounts = listOf(SampleDataProvider().accountWithTransferableResourceLsu).toPersistentList(),
            validators = persistentListOf(),
            validatorSectionTitleText = "Staking to Validators".uppercase()
        )
    }
}
