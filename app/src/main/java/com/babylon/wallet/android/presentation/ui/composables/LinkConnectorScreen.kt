package com.babylon.wallet.android.presentation.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.themedColorTint
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun LinkConnectorScreen(
    modifier: Modifier = Modifier,
    backIconType: BackIconType = BackIconType.Back,
    onLinkConnectorClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    BackHandler(onBack = onCloseClick)

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = com.babylon.wallet.android.R.string.empty),
                onBackClick = onCloseClick,
                backIconType = backIconType,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingXXLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(id = R.drawable.ic_radix_connect),
                tint = themedColorTint(),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
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

@Preview
@Composable
fun LinkConnectorSectionPreviewLight() {
    RadixWalletPreviewTheme {
        LinkConnectorScreen(
            modifier = Modifier.fillMaxSize(),
            onLinkConnectorClick = {},
            onCloseClick = {}
        )
    }
}

@Preview
@Composable
fun LinkConnectorSectionPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        LinkConnectorScreen(
            modifier = Modifier.fillMaxSize(),
            onLinkConnectorClick = {},
            onCloseClick = {}
        )
    }
}
