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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun DevSettingsScreen(
    viewModel: DevSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    DevSettingsContent(
        modifier = modifier.navigationBarsPadding(),
        onBackClick = onBackClick,
        isAuthSigningLoading = state.isLoading,
        hasAuthKey = state.hasAuthKey,
        onCreateAndUploadAuthKey = {
            context.biometricAuthenticate { result ->
                if (result == BiometricAuthenticationResult.Succeeded) {
                    viewModel.onCreateAndUploadAuthKey()
                }
            }
        }
    )
}

@Composable
private fun DevSettingsContent(
    onBackClick: () -> Unit,
    isAuthSigningLoading: Boolean,
    modifier: Modifier = Modifier,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.accountSettings_devPreferences),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
        ) {
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
fun DevSettingsPreview() {
    RadixWalletTheme {
        DevSettingsContent(
            onBackClick = {},
            isAuthSigningLoading = false,
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {}
        )
    }
}
