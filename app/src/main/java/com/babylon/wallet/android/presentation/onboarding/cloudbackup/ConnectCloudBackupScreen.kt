package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.onboarding.cloudbackup.ConnectCloudBackupViewModel.ConnectMode
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.rememberLauncherForSignInToGoogle

@Composable
fun ConnectCloudBackupScreen(
    modifier: Modifier = Modifier,
    viewModel: ConnectCloudBackupViewModel,
    onBackClick: () -> Unit,
    onProceed: (mode: ConnectMode, isCloudBackupEnabled: Boolean) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBackClick() }

    val signInLauncher = rememberLauncherForSignInToGoogle(viewModel = viewModel)

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ConnectCloudBackupViewModel.Event.SignInToGoogle -> signInLauncher.launch(Unit)
                is ConnectCloudBackupViewModel.Event.Proceed -> onProceed(event.mode, event.isCloudBackupEnabled)
            }
        }
    }

    ConnectCloudBackupContent(
        modifier = modifier,
        state = state,
        onErrorMessageShown = viewModel::onErrorMessageShown,
        onBackClick = onBackClick,
        onLoginToGoogleClick = viewModel::onLoginToGoogleClick,
        onSkipClick = viewModel::onSkipClick
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun ConnectCloudBackupContent(
    modifier: Modifier = Modifier,
    state: ConnectCloudBackupViewModel.State,
    onErrorMessageShown: () -> Unit,
    onBackClick: () -> Unit,
    onLoginToGoogleClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.errorMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onErrorMessageShown
    )

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                actions = {
                    RadixTextButton(
                        text = when (state.mode) {
                            ConnectMode.NewWallet -> stringResource(id = R.string.onboarding_cloudAndroid_skip)
                            ConnectMode.RestoreWallet -> stringResource(id = R.string.onboarding_cloudRestoreAndroid_skip)
                            ConnectMode.ExistingWallet -> stringResource(id = R.string.empty) // TODO test it
                        },
                        onClick = {
                            onSkipClick() // continue without back up
                        }
                    )
                },
                backIconType = when (state.mode) {
                    ConnectMode.NewWallet -> BackIconType.Close
                    ConnectMode.RestoreWallet -> BackIconType.Back
                    ConnectMode.ExistingWallet -> BackIconType.Back
                },
                windowInsets = WindowInsets.statusBars,
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_backup_google),
                tint = Color.Unspecified,
                contentDescription = null
            )

            Spacer(modifier = Modifier.weight(0.1f))
            Text(
                modifier = Modifier
                    .widthIn(max = 250.dp)
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                text = when (state.mode) {
                    ConnectMode.NewWallet -> stringResource(id = R.string.onboarding_cloudAndroid_backupTitle)
                    ConnectMode.RestoreWallet -> stringResource(id = R.string.onboarding_cloudRestoreAndroid_backupTitle)
                    ConnectMode.ExistingWallet -> "Backups on Google Drive Have Updated"
                },
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier.widthIn(max = 300.dp),
                text = when (state.mode) {
                    ConnectMode.NewWallet -> stringResource(id = R.string.onboarding_cloudAndroid_backupSubtitle)
                    ConnectMode.RestoreWallet -> stringResource(id = R.string.onboarding_cloudRestoreAndroid_backupSubtitle)
                    ConnectMode.ExistingWallet ->
                        "The Radix Wallet has an all new and improved backup system.\n" +
                            "\n" +
                            "To continue, log in with the Google Drive account you want to use for backups."
                },
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(0.4f))
            RadixPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = when (state.mode) {
                    ConnectMode.NewWallet -> stringResource(id = R.string.onboarding_cloudAndroid_backupButton)
                    ConnectMode.RestoreWallet -> stringResource(id = R.string.onboarding_cloudRestoreAndroid_loginButton)
                    ConnectMode.ExistingWallet -> "Login to Google Drive for Backups"
                },
                isLoading = state.isConnecting,
                onClick = onLoginToGoogleClick
            )
            if (state.mode == ConnectMode.ExistingWallet) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Skip for now",
                    onClick = onSkipClick
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Preview
@Composable
fun ConnectCloudBackupScreenNewWalletPreview() {
    RadixWalletPreviewTheme {
        ConnectCloudBackupContent(
            state = ConnectCloudBackupViewModel.State(mode = ConnectMode.NewWallet),
            onErrorMessageShown = {},
            onBackClick = {},
            onLoginToGoogleClick = {},
            onSkipClick = {}
        )
    }
}

@Preview
@Composable
fun ConnectCloudBackupScreenRestoreWalletPreview() {
    RadixWalletPreviewTheme {
        ConnectCloudBackupContent(
            state = ConnectCloudBackupViewModel.State(mode = ConnectMode.RestoreWallet),
            onErrorMessageShown = {},
            onBackClick = {},
            onLoginToGoogleClick = {},
            onSkipClick = {}
        )
    }
}

@Preview
@Composable
fun ConnectCloudBackupScreenExistingWalletPreview() {
    RadixWalletPreviewTheme {
        ConnectCloudBackupContent(
            state = ConnectCloudBackupViewModel.State(mode = ConnectMode.ExistingWallet),
            onErrorMessageShown = {},
            onBackClick = {},
            onLoginToGoogleClick = {},
            onSkipClick = {}
        )
    }
}
