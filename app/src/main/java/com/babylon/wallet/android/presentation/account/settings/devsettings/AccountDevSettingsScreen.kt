package com.babylon.wallet.android.presentation.account.settings.devsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun AccountDevSettingsScreen(
    viewModel: DevAccountSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountDevSettingsContent(
        modifier = modifier.navigationBarsPadding(),
        onBackClick = onBackClick,
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        faucetState = state.faucetState,
        isXrdLoading = state.isFreeXRDLoading,
        isAuthSigningLoading = state.isLoading,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        hasAuthKey = state.hasAuthKey,
        onCreateAndUploadAuthKey = {
            context.biometricAuthenticate {
                if (it) {
                    viewModel.onCreateAndUploadAuthKey()
                }
            }
        }
    )
}

@Composable
private fun AccountDevSettingsContent(
    onBackClick: () -> Unit,
    onGetFreeXrdClick: () -> Unit,
    faucetState: FaucetState,
    isXrdLoading: Boolean,
    isAuthSigningLoading: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = error,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.accountSettings_devPreferences),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
        ) {
            if (faucetState is FaucetState.Available) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(R.string.accountSettings_getXrdTestTokens),
                    onClick = {
                        context.biometricAuthenticate { authenticatedSuccessfully ->
                            if (authenticatedSuccessfully) {
                                onGetFreeXrdClick()
                            }
                        }
                    },
                    isLoading = isXrdLoading,
                    enabled = !isXrdLoading && faucetState.isEnabled
                )
            }
            if (isXrdLoading) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(R.string.accountSettings_loadingPrompt),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1,
                )
            }
            if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED && !hasAuthKey) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.biometrics_prompt_createSignAuthKey),
                    onClick = onCreateAndUploadAuthKey,
                    isLoading = isAuthSigningLoading,
                    enabled = !isAuthSigningLoading,
                    throttleClicks = true
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingsPreview() {
    RadixWalletTheme {
        AccountDevSettingsContent(
            onBackClick = {},
            onGetFreeXrdClick = {},
            faucetState = FaucetState.Available(isEnabled = true),
            isXrdLoading = false,
            isAuthSigningLoading = false,
            onMessageShown = {},
            error = null,
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {}
        )
    }
}
