package com.babylon.wallet.android.presentation.settings.troubleshooting.importlegacywallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.Gray4
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.gradient
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.profile.olympiaimport.OlympiaAccountType

@Composable
fun LegacyAccountCard(
    accountName: String,
    accountType: OlympiaAccountType,
    address: LegacyOlympiaAccountAddress,
    newAddress: AccountAddress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = RadixTheme.dimensions.paddingSemiLarge,
                vertical = RadixTheme.dimensions.paddingLarge
            )
    ) {
        Text(
            text = accountName,
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.secondaryHeader,
            color = White
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
        Text(
            text = stringResource(
                id = when (accountType) {
                    OlympiaAccountType.Hardware -> R.string.importOlympiaAccounts_accountsToImport_ledgerAccount
                    OlympiaAccountType.Software -> R.string.importOlympiaAccounts_accountsToImport_legacyAccount
                }
            ),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = Gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = stringResource(id = R.string.importOlympiaAccounts_accountsToImport_olympiaAddressLabel),
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.body2Header,
            color = White
        )
        Text(
            text = address.formatted(),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = Gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Text(
            text = stringResource(id = R.string.importOlympiaAccounts_accountsToImport_newAddressLabel),
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.body2Header,
            color = White
        )
        Text(
            text = newAddress.formatted(),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = Gray4
        )
    }
}

@UsesSampleValues
@Preview(showBackground = false)
@Composable
fun LegacyAccountCardPreview() {
    RadixWalletTheme {
        LegacyAccountCard(
            modifier = Modifier
                .background(
                    AppearanceId(1u).gradient(),
                    shape = RadixTheme.shapes.roundedRectSmall
                ),
            accountName = "Account name",
            accountType = OlympiaAccountType.Software,
            address = LegacyOlympiaAccountAddress.sample(),
            newAddress = AccountAddress.sampleMainnet.random()
        )
    }
}
