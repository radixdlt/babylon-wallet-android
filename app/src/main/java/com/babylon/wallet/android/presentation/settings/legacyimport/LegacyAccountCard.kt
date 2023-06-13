package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.utils.truncatedHash
import rdx.works.profile.olympiaimport.OlympiaAccountType

@Composable
fun LegacyAccountCard(
    accountName: String,
    accountType: OlympiaAccountType,
    address: String,
    newAddress: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            text = accountName,
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.secondaryHeader,
            color = Color.White
        )
        Text(
            text = stringResource(
                id = when (accountType) {
                    OlympiaAccountType.Hardware -> R.string.importLegacyWallet_accountsToImport_ledgerAccount
                    OlympiaAccountType.Software -> R.string.importLegacyWallet_accountsToImport_legacyAccount
                }
            ),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = stringResource(id = R.string.importLegacyWallet_selectAccountsToImport_olympiaAddress),
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.secondaryHeader,
            color = Color.White
        )
        Text(
            text = address.truncatedHash(),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            text = stringResource(id = R.string.importLegacyWallet_accountsToImport_newAddress),
            textAlign = TextAlign.Start,
            maxLines = 1,
            style = RadixTheme.typography.secondaryHeader,
            color = Color.White
        )
        Text(
            text = newAddress.truncatedHash(),
            textAlign = TextAlign.Start,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray4
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LegacyAccountCardPreview() {
    RadixWalletTheme {
        LegacyAccountCard(
            accountName = "Account name",
            accountType = OlympiaAccountType.Software,
            address = "jf932j9f32o",
            newAddress = "test/path"
        )
    }
}
