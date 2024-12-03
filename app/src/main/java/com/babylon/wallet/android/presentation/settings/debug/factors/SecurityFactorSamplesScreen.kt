package com.babylon.wallet.android.presentation.settings.debug.factors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SecurityFactorSamplesScreen(
    viewModel: SecurityFactorSamplesViewModel,
    onBackClick: () -> Unit
) {
    val state = viewModel.state.collectAsState().value

    SecurityFactorSamplesContent(
        state = state,
        onBackClick = onBackClick
    )
}

@Composable
private fun SecurityFactorSamplesContent(
    modifier: Modifier = Modifier,
    state: SecurityFactorSamplesViewModel.State,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.securityFactors_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {

        }
    }
}

@Composable
@Preview
private fun SecurityFactorSamplesPreview() {
    RadixWalletPreviewTheme {
        SecurityFactorSamplesContent(
            state = SecurityFactorSamplesViewModel.State(),
            onBackClick = {}
        )
    }
}