package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.composable.RadixCheckboxDefaults
import com.babylon.wallet.android.designsystem.composable.RadixRadioButton
import com.babylon.wallet.android.designsystem.composable.RadixRadioButtonDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun AccountSelectionCard(
    accountName: String,
    address: AccountAddress,
    checked: Boolean,
    isSingleChoice: Boolean,
    radioButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabledForSelection: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (accountName.isNotBlank()) {
                Text(
                    text = accountName,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    style = RadixTheme.typography.body1Header,
                    color = White
                )
            }

            ActionableAddressView(
                address = Address.Account(address),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = White.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.weight(0.1f))
        if (isSingleChoice) {
            RadixRadioButton(
                selected = checked,
                onClick = radioButtonClicked,
                enabled = isEnabledForSelection,
                colors = RadixRadioButtonDefaults.onLightBackgroundColors()
            )
        } else {
            Checkbox(
                checked = checked,
                colors = RadixCheckboxDefaults.onLightBackgroundColors(),
                onCheckedChange = null
            )
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun DAppAccountCardPreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = AccountAddress.sampleMainnet.random(),
            isSingleChoice = false,
            radioButtonClicked = {},
            checked = true,
            isEnabledForSelection = true
        )
    }
}

@UsesSampleValues
@Preview
@Preview("large font", fontScale = 2f)
@Composable
fun DAppAccountCardLargeFontPreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = AccountAddress.sampleMainnet.random(),
            isSingleChoice = false,
            radioButtonClicked = {},
            checked = true,
            isEnabledForSelection = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun DAppAccountCardSingleChoicePreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = AccountAddress.sampleMainnet.random(),
            isSingleChoice = true,
            radioButtonClicked = {},
            checked = true,
            isEnabledForSelection = true
        )
    }
}
