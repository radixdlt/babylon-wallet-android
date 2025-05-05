package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.SeedPhraseInputVerificationForm
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun ConfirmSeedPhraseScreen(
    viewModel: ConfirmSeedPhraseViewModel,
    onMnemonicBackedUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    ConfirmSeedPhraseContent(
        state = state,
        onBackClick = onDismiss,
        onSubmitClick = {
            context.biometricAuthenticate { result ->
                if (result == BiometricAuthenticationResult.Succeeded) {
                    viewModel.onSubmit()
                }
            }
        },
        onWordTyped = viewModel::onWordChanged,
        onMessageShown = viewModel::onMessageShown
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ConfirmSeedPhraseViewModel.Event.MnemonicBackedUp -> onMnemonicBackedUp()
            }
        }
    }
}

@Composable
private fun ConfirmSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: ConfirmSeedPhraseViewModel.State,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onWordTyped: (Int, String) -> Unit,
    onMessageShown: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                text = stringResource(id = R.string.common_continue),
                onClick = onSubmitClick,
                enabled = state.seedPhraseState.allFieldsHaveValue,
                insets = WindowInsets.navigationBars.union(WindowInsets.ime)
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        SeedPhraseView(
            modifier = Modifier.padding(padding),
            state = state,
            onWordChanged = onWordTyped
        )
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    state: ConfirmSeedPhraseViewModel.State,
    onWordChanged: (Int, String) -> Unit
) {
    SecureScreen()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.confirmMnemonicBackedUp_title),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.confirmMnemonicBackedUp_subtitle),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        SeedPhraseInputVerificationForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            seedPhraseWords = state.seedPhraseState.seedPhraseWords,
            onWordChanged = onWordChanged
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun ConfirmSeedPhrasePreviewLight() {
    RadixWalletPreviewTheme {
        ConfirmSeedPhraseContent(
            state = ConfirmSeedPhraseViewModel.State(
                factorSource = FactorSource.Device.sample()
            ),
            onBackClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onMessageShown = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun ConfirmSeedPhrasePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ConfirmSeedPhraseContent(
            state = ConfirmSeedPhraseViewModel.State(
                factorSource = FactorSource.Device.sample()
            ),
            onBackClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onMessageShown = {}
        )
    }
}