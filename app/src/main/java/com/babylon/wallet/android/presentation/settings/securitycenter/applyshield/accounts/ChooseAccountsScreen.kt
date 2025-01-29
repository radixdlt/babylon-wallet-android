package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun ChooseAccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseAccountsViewModel,
    onDismiss: () -> Unit
) {
    ChooseAccountsContent(
        modifier = modifier,
        onDismiss = onDismiss,
        onContinueClick = {}
    )
}

@Composable
private fun ChooseAccountsContent(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(R.string.common_continue)
            )
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        }
    }
}

@Composable
@Preview
private fun ChooseAccountsPreview() {
    RadixWalletPreviewTheme {
        ChooseAccountsContent(
            onDismiss = {},
            onContinueClick = {}
        )
    }
}
