package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun ApplyShieldScreen(
    modifier: Modifier = Modifier,
    viewModel: ApplyShieldViewModel,
    onDismiss: () -> Unit
) {
    ApplyShieldContent(
        modifier = modifier,
        onDismiss = onDismiss,
        onApplyClick = {}
    )
}

@Composable
private fun ApplyShieldContent(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onApplyClick: () -> Unit
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
                onClick = onApplyClick,
                text = "Save and Apply"
            )
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        }
    }
}

@Composable
@Preview
private fun ApplyShieldPreview() {
    RadixWalletPreviewTheme {
        ApplyShieldContent(
            onDismiss = {},
            onApplyClick = {}
        )
    }
}
