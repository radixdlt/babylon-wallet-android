package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import kotlinx.collections.immutable.PersistentList

@Composable
fun ApplyShieldScreen(
    modifier: Modifier = Modifier,
    securityStructureId: SecurityStructureId,
    entityAddresses: PersistentList<AddressOfAccountOrPersona>,
    viewModel: ApplyShieldViewModel,
    onDismiss: () -> Unit,
    onShieldApplied: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ApplyShieldContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onMessageShown = viewModel::onMessageShown,
        onApplyClick = { viewModel.onApplyClick(securityStructureId, entityAddresses) }
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ApplyShieldViewModel.Event -> onShieldApplied()
            }
        }
    }
}

@Composable
private fun ApplyShieldContent(
    modifier: Modifier = Modifier,
    state: ApplyShieldViewModel.State,
    onDismiss: () -> Unit,
    onMessageShown: () -> Unit,
    onApplyClick: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.message,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

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
                text = "Save and Apply", // TODO crowdin
                isLoading = state.isLoading,
                enabled = !state.isLoading,
                onClick = onApplyClick
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.shield_intro_1),
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = "Apply your Shield", // TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = "Now let’s save your Shield settings to your wallet and apply them on the Radix Network with a transaction.", // TODO crowdin
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
@Preview
private fun ApplyShieldPreview() {
    RadixWalletPreviewTheme {
        ApplyShieldContent(
            state = ApplyShieldViewModel.State(),
            onDismiss = {},
            onMessageShown = {},
            onApplyClick = {}
        )
    }
}
