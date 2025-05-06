package com.babylon.wallet.android.presentation.account.settings.specificassets.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.designsystem.composable.RadixRadioButton
import com.babylon.wallet.android.designsystem.composable.RadixRadioButtonDefaults

@Composable
fun LabeledRadioButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = modifier.clickable {
            onSelected()
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadixRadioButton(
            selected = selected,
            colors = RadixRadioButtonDefaults.colors(),
            onClick = onSelected,
        )
        Text(
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
private fun LabeledRadioButtonPreview() {
    RadixWalletPreviewTheme {
        LabeledRadioButton(
            modifier = Modifier,
            label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetAllow),
            selected = true,
            onSelected = {}
        )
    }
}
