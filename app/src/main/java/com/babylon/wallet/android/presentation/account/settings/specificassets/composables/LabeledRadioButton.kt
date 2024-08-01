package com.babylon.wallet.android.presentation.account.settings.specificassets.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun LabeledRadioButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Box(
        modifier = modifier.clickable {
            onSelected()
        },
        contentAlignment = Alignment.CenterStart
    ) {
        RadioButton(
            selected = selected,
            colors = RadioButtonDefaults.colors(
                selectedColor = RadixTheme.colors.gray1,
                unselectedColor = RadixTheme.colors.gray3,
                disabledSelectedColor = RadixTheme.colors.white
            ),
            onClick = onSelected,
        )
        Text(
            modifier = Modifier.padding(start = 40.dp),
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
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
