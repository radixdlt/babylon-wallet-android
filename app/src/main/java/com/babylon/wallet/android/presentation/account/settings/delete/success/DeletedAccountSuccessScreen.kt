package com.babylon.wallet.android.presentation.account.settings.delete.success

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun DeleteAccountSuccessScreen(
    modifier: Modifier = Modifier,
    onGotoHomescreen: () -> Unit
) {
    DeleteAccountSuccessContent(
        modifier = modifier,
        onGotoHomescreen = onGotoHomescreen
    )
}

@Composable
private fun DeleteAccountSuccessContent(
    modifier: Modifier = Modifier,
    onGotoHomescreen: () -> Unit
) {
    BackHandler {
        // Consume back events
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                windowInsets = WindowInsets.statusBarsAndBanner,
                backIconType = BackIconType.None,
                onBackClick = {}
            )
        },
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.accountSettings_accountDeleted_button),
                        onClick = onGotoHomescreen
                    )
                },
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Icon(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                painter = painterResource(com.babylon.wallet.android.designsystem.R.drawable.ic_account_delete),
                contentDescription = null,
                tint = RadixTheme.colors.gray2
            )

            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = RadixTheme.dimensions.paddingXXXLarge)
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(id = R.string.accountSettings_accountDeleted_title),
                style = RadixTheme.typography.title,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingLarge)
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.accountSettings_accountDeleted_message),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular
            )
        }
    }
}

@Composable
@Preview
fun DeletedAccountSuccessPreview() {
    RadixWalletPreviewTheme {
        DeleteAccountSuccessContent(onGotoHomescreen = {})
    }
}
