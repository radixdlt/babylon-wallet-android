package com.babylon.wallet.android.presentation.account.settings.delete

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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun AccountDeleteSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.padding(
                start = RadixTheme.dimensions.paddingXSmall,
                top = RadixTheme.dimensions.paddingMedium
            ),
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
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
            text = "Delete this Account", // TODO crowdin
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = "Delete this Account in your wallet? You *will not be able to access this account again*".formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
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

            WarningButton(
                modifier = Modifier.weight(1.5f),
                text = "Delete account",
                onClick = onDeleteAccount
            )
        }
    }
}


@UsesSampleValues
@Preview
@Composable
fun AccountDeleteSheetPreview() {
    RadixWalletPreviewTheme {
        AccountDeleteSheet(
            onClose = {},
            onDeleteAccount = {}
        )
    }
}