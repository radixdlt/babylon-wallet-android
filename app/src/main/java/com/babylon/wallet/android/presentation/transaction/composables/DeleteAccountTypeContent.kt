package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.DSR
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun DeleteAccountTypeContent(
    modifier: Modifier = Modifier,
    preview: PreviewType.DeleteAccount,
    hiddenResourceIds: PersistentList<ResourceIdentifier>,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        SectionTitle(
            title = stringResource(id = R.string.transactionReview_deletingAccount_title),
            iconRes = DSR.ic_account_delete_small
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = RadixTheme.colors.background,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium),
        ) {
            AccountCardHeader(account = InvolvedAccount.Owned(preview.deletingAccount))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.backgroundSecondary,
                        shape = RadixTheme.shapes.roundedRectBottomMedium
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingSemiLarge
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = DSR.ic_delete_outline),
                    contentDescription = null,
                    tint = RadixTheme.colors.error
                )

                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))

                Text(
                    text = stringResource(id = R.string.transactionReview_deletingAccount_message),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.error
                )
            }
        }

        if (preview.to != null) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

            DepositAccountContent(
                to = remember(preview.to) { persistentListOf(preview.to) },
                hiddenResourceIds = hiddenResourceIds,
                onEditGuaranteesClick = {},
                onTransferableFungibleClick = onTransferableFungibleClick,
                onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick,
                onTransferableNonFungibleByAmountClick = { _, _ -> }
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
@UsesSampleValues
private fun DeleteAccountTypePreview() {
    RadixWalletPreviewTheme {
        DeleteAccountTypeContent(
            preview = PreviewType.DeleteAccount(
                deletingAccount = Account.sampleMainnet(),
                to = AccountWithTransferables(
                    account = InvolvedAccount.Owned(Account.sampleMainnet()),
                    transferables = listOf(
                        Transferable.FungibleType.Token(
                            asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                            amount = BoundedAmount.Exact("745".toDecimal192()),
                            isNewlyCreated = true
                        )
                    )
                )
            ),
            hiddenResourceIds = persistentListOf(),
            onTransferableFungibleClick = {},
            onTransferableNonFungibleItemClick = { _, _ -> }
        )
    }
}
