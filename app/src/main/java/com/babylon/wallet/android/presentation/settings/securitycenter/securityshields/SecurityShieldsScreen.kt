package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SecurityShieldsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityShieldsViewModel,
    onBackClick: () -> Unit,
    onCreateShieldClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SecurityShieldsContent(
        modifier = modifier,
        state = state,
        onCreateShieldClick = onCreateShieldClick,
        onBackClick = onBackClick
    )
}

@Composable
fun SecurityShieldsContent(
    modifier: Modifier = Modifier,
    state: SecurityShieldsViewModel.State,
    onBackClick: () -> Unit,
    onCreateShieldClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "Security Shields", // TODO crowdin
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner,
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingLarge
                        ),
                    text = "Create New Security shield", // TODO crowdin
                    onClick = onCreateShieldClick,
                    throttleClicks = true
                )
            }
        }
    }
}

@Composable
@Preview
private fun SecurityShieldsPreview() {
    RadixWalletPreviewTheme {
        SecurityShieldsContent(
            state = SecurityShieldsViewModel.State(),
            onBackClick = {},
            onCreateShieldClick = {}
        )
    }
}
