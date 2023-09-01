package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel
import kotlinx.collections.immutable.toPersistentList

@Composable
fun TransactionPreviewTypeContent(
    modifier: Modifier = Modifier,
    state: TransactionApprovalViewModel.State,
    preview: PreviewType.Transaction,
    onPromptForGuarantees: () -> Unit,
    onDappClick: (DAppWithMetadataAndAssociatedResources) -> Unit
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
                from = preview.from.toPersistentList(),
                showStrokeLine = preview.from.toPersistentList().isNotEmpty() ||
                    preview.dApps.toPersistentList().isNotEmpty()
            )

            ConnectedDAppsContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                connectedDApps = preview.dApps.toPersistentList(),
                onDAppClick = onDappClick,
                showStrokeLine = preview.dApps.toPersistentList().isNotEmpty()
            )

            DepositAccountContent(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                to = preview.to.toPersistentList(),
                promptForGuarantees = onPromptForGuarantees,
                showStrokeLine = preview.from.toPersistentList().isNotEmpty() ||
                    preview.dApps.toPersistentList().isNotEmpty()
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        PresentingProofsContent(
            badges = preview.badges.toPersistentList()
        )
    }
}
