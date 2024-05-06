package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage

@Composable
fun CloudBackupLoginScreen(
    modifier: Modifier = Modifier,
    viewModel: CloudBackupLoginViewModel,
    onBackClick: () -> Unit,
    onContinueToRestoreFromBackup: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBackClick() }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleSignInResult(result)
    }

    val recoverDriveAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleAuthDriveResult(result)
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent
            .collect { event ->
                when (event) {
                    is CloudBackupLoginViewModel.Event.SignInToGoogle -> {
                        signInLauncher.launch(event.signInIntent)
                    }

                    is CloudBackupLoginViewModel.Event.RecoverUserAuthToDrive -> {
                        recoverDriveAuthLauncher.launch(event.authIntent)
                    }

                    is CloudBackupLoginViewModel.Event.ProceedToRestoreFromBackup -> {
                        onContinueToRestoreFromBackup() // navigate to RestoreFromBackupScreen
                    }
                }
            }
    }

    CloudBackupLoginContent(
        modifier = modifier,
        errorMessage = state.errorMessage,
        onErrorMessageShown = viewModel::onErrorMessageShown,
        onBackClick = onBackClick,
        onLoginToGoogleClick = viewModel::onLoginToGoogleClick,
        onSkipClick = viewModel::onSkipClick
    )
}

@Composable
private fun CloudBackupLoginContent(
    modifier: Modifier = Modifier,
    errorMessage: UiMessage?,
    onErrorMessageShown: () -> Unit,
    onBackClick: () -> Unit,
    onLoginToGoogleClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = errorMessage,
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
                        text = "Skip", // TODO Crowdin
                        onClick = onSkipClick
                    )
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
                text = "Restore wallet from backup", // TODO Crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier.widthIn(max = 300.dp),
                text = "Log in to Google Drive to restore your Radix Wallet from backup.", // TODO Crowdin
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray2,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(0.4f))
            RadixPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Log in to Google Drive", // TODO Crowdin
                onClick = onLoginToGoogleClick
            )
            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Preview
@Composable
fun CloudBackupLoginPreview() {
    RadixWalletPreviewTheme {
        CloudBackupLoginContent(
            errorMessage = null,
            onErrorMessageShown = {},
            onBackClick = {},
            onLoginToGoogleClick = {},
            onSkipClick = {}
        )
    }
}
