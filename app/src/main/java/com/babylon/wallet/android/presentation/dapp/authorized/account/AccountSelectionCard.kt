package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView

@Composable
fun AccountSelectionCard(
    accountName: String,
    address: String,
    checked: Boolean,
    isSingleChoice: Boolean,
    radioButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = accountName,
                textAlign = TextAlign.Start,
                maxLines = 2,
                style = RadixTheme.typography.body2Header,
                color = Color.White
            )

            ActionableAddressView(
                address = address,
                textStyle = RadixTheme.typography.body2Link,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.weight(0.1f))
        if (isSingleChoice) {
            RadioButton(
                selected = checked,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RadixTheme.colors.gray1,
                    unselectedColor = RadixTheme.colors.gray3,
                    disabledSelectedColor = Color.White
                ),
                onClick = radioButtonClicked,
            )
        } else {
            Checkbox(
                checked = checked,
                colors = CheckboxDefaults.colors(
                    checkedColor = RadixTheme.colors.gray1,
                    uncheckedColor = RadixTheme.colors.gray3,
                    checkmarkColor = Color.White
                ),
                onCheckedChange = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DAppAccountCardPreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = "jf932j9f32o",
            isSingleChoice = false,
            radioButtonClicked = {},
            checked = true
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f)
@Composable
fun DAppAccountCardLargeFontPreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = "jf932j9f32o",
            isSingleChoice = false,
            radioButtonClicked = {},
            checked = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DAppAccountCardSingleChoicePreview() {
    RadixWalletTheme {
        AccountSelectionCard(
            accountName = "Account name",
            address = "jf932j9f32o",
            isSingleChoice = true,
            radioButtonClicked = {},
            checked = true
        )
    }
}
