package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun LinkConnectorScreen(
    modifier: Modifier,
    backIconType: BackIconType = BackIconType.Back,
    onLinkConnectorClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = modifier.background(RadixTheme.colors.defaultBackground)) {
        RadixCenteredTopAppBar(
            title = stringResource(id = com.babylon.wallet.android.R.string.empty),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            backIconType = backIconType
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingXLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(id = R.drawable.ic_radix_connect),
                tint = Color.Unspecified,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            RadixPrimaryButton(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                onClick = onLinkConnectorClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LinkConnectorSectionPreview() {
    RadixWalletTheme {
        LinkConnectorScreen(
            modifier = Modifier.fillMaxSize(),
            onLinkConnectorClick = {},
            onBackClick = {}
        )
    }
}
