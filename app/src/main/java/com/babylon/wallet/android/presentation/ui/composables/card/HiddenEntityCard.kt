package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun HiddenEntityCard(
    modifier: Modifier = Modifier,
    shape: Shape = RadixTheme.shapes.roundedRectMedium
) {
    Row(
        modifier = modifier
            .background(
                color = RadixTheme.colors.gray4,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.common_hiddenAccountsOrPersonas),
            style = RadixTheme.typography.body1HighImportance,
            maxLines = 1,
            color = RadixTheme.colors.gray2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun HiddenEntityCardPreview() {
    RadixWalletPreviewTheme {
        HiddenEntityCard()
    }
}
