package com.babylon.wallet.android.presentation.account.recover.complete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun RecoveryScanCompleteScreen(
    onContinueClick: () -> Unit
) {
    RecoveryScanCompleteContent(onContinueClick = onContinueClick)
}

@Composable
private fun RecoveryScanCompleteContent(
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit
) {
    BackHandler {}
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = "",
                onBackClick = {},
                backIconType = BackIconType.None
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.recoverWalletWithoutProfileComplete_header_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverWalletWithoutProfileComplete_header_subtitle).formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.weight(1f))
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.common_continue),
                onClick = onContinueClick
            )
        }
    }
}

@Preview
@Composable
fun RestoreWithoutBackupPreview() {
    RadixWalletTheme {
        RecoveryScanCompleteContent(
            onContinueClick = {}
        )
    }
}
