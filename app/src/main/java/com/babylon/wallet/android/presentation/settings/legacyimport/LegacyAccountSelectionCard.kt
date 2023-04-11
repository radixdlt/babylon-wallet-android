package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun LegacyAccountSelectionCard(
    accountName: String,
    address: String,
    path: String,
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
                Text(
                    text = stringResource(id = R.string.name),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = RadixTheme.typography.body2Header,
                    color = Color.White
                )
                Text(
                    text = accountName,
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body2Regular,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Column {
                Text(
                    text = stringResource(id = R.string.olympia_address),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = RadixTheme.typography.body2Header,
                    color = Color.White
                )
                Text(
                    text = address,
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body2Regular,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
                Text(
                    text = stringResource(id = R.string.path),
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body2Header,
                    color = Color.White
                )
                Text(
                    text = path,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = RadixTheme.typography.body2Regular,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

        }
        Spacer(modifier = Modifier.weight(0.1f))
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

@Preview(showBackground = true)
@Composable
fun LegacyAccountSelectionCardPreview() {
    RadixWalletTheme {
        LegacyAccountSelectionCard(
            accountName = "Account name",
            address = "jf932j9f32o",
            checked = true,
            path = "test/path"
        )
    }
}
