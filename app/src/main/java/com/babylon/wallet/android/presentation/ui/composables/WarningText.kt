package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun WarningText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    textStyle: TextStyle = RadixTheme.typography.body1StandaloneLink,
    contentColor: Color = RadixTheme.colors.warning,
    iconRes: Int = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = RadixTheme.dimensions.paddingSmall,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        Icon(
            painter = painterResource(
                id = iconRes
            ),
            contentDescription = null,
            tint = contentColor
        )
        Text(
            text = text,
            style = textStyle,
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WarningTextPreview() {
    RadixWalletPreviewTheme {
        WarningText(text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure)))
    }
}
