package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun ConfirmDeviceSeedPhraseScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfirmDeviceSeedPhraseViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfirmDeviceSeedPhraseContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss
    )
}

@Composable
private fun ConfirmDeviceSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: ConfirmDeviceSeedPhraseViewModel.State,
    onDismiss: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = { onDismiss() },
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
        bottomBar = {
            RadixBottomBar(
                onClick = {},
                text = stringResource(id = R.string.common_confirm)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Confirm Seed Phrase",
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@UsesSampleValues
@Composable
@Preview
private fun ConfirmDeviceSeedPhrasePreview() {
    RadixWalletPreviewTheme {
        ConfirmDeviceSeedPhraseContent(
            state = ConfirmDeviceSeedPhraseViewModel.State(),
            onDismiss = {}
        )
    }
}