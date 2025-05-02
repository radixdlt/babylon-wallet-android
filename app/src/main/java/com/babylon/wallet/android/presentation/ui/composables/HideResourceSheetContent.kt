package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun HideResourceSheetContent(
    title: String,
    description: String,
    positiveButton: String,
    onPositiveButtonClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.padding(
                start = RadixTheme.dimensions.paddingXXSmall,
                top = RadixTheme.dimensions.paddingMedium
            ),
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.icon,
                contentDescription = null
            )
        }

        Image(
            modifier = Modifier
                .size(51.dp)
                .align(Alignment.CenterHorizontally),
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_show),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = description.formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RadixTheme.dimensions.paddingSemiLarge,
                    end = RadixTheme.dimensions.paddingSemiLarge,
                    bottom = RadixTheme.dimensions.paddingXXLarge
                )
        ) {
            RadixSecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.common_cancel),
                onClick = onClose
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            RadixPrimaryButton(
                modifier = Modifier.weight(1.5f),
                text = positiveButton,
                onClick = onPositiveButtonClick
            )
        }
    }
}

@Composable
@Preview
private fun HideResourceSheetContentPreview() {
    RadixWalletPreviewTheme {
        HideResourceSheetContent(
            title = stringResource(id = R.string.accountSettings_hideThisAccount),
            description = stringResource(id = R.string.accountSettings_hideAccount_message),
            positiveButton = stringResource(id = R.string.accountSettings_hideAccount_button),
            onPositiveButtonClick = {},
            onClose = {}
        )
    }
}
