package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun DeleteAccountContent(
    modifier: Modifier = Modifier,
    account: Account
) {
    Column(
        modifier = modifier
    ) {
        SectionTitle(
            titleRes = R.string.transactionReview_deletingAccountHeading,
            iconRes = DSR.ic_account_delete_small
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = RadixTheme.colors.defaultBackground,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium),
        ) {
            AccountCardHeader(
                displayName = account.displayName.value,
                address = account.address
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.gray5,
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
                    tint = RadixTheme.colors.red1
                )

                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))

                Text(
                    text = stringResource(id = R.string.transactionReview_deletingAccountDescription),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.red1
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@UsesSampleValues
private fun DeleteAccountTypePreview() {
    RadixWalletPreviewTheme {
        DeleteAccountContent(
            account = Account.sampleMainnet()
        )
    }
}
