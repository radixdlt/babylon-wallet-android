package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.ui.composables.assets.strokeLine
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun CommonTransferContent(
    modifier: Modifier = Modifier,
    state: TransactionReviewViewModel.State,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
    previewType: PreviewType.Transfer,
    onPromptForGuarantees: () -> Unit,
    middleSection: @Composable () -> Unit
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
                from = previewType.from.toPersistentList(),
                onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                    onFungibleResourceClick(fungibleResource, isNewlyCreated)
                },
                onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                    onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                }
            )

            Column(
                modifier = Modifier
                    .applyIf(condition = state.showDottedLine, modifier = Modifier.strokeLine())
                    .padding(top = RadixTheme.dimensions.paddingXLarge)
            ) {
                middleSection()

                DepositAccountContent(
                    modifier = Modifier.padding(
                        start = RadixTheme.dimensions.paddingDefault,
                        end = RadixTheme.dimensions.paddingDefault
                    ),
                    to = previewType.to.toPersistentList(),
                    promptForGuarantees = onPromptForGuarantees,
                    onFungibleResourceClick = { fungibleResource, isNewlyCreated ->
                        onFungibleResourceClick(fungibleResource, isNewlyCreated)
                    },
                    onNonFungibleResourceClick = { nonFungibleResource, nonFungibleResourceItem, isNewlyCreated ->
                        onNonFungibleResourceClick(nonFungibleResource, nonFungibleResourceItem, isNewlyCreated)
                    }
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
}
