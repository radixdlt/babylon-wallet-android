package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun AccountPreferenceScreen(
    viewModel: AccountPreferenceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state
    AccountPreferenceContent(
        onBackClick = onBackClick,
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        canUseFaucet = state.canUseFaucet,
        loading = state.isLoading,
        isDeviceSecure = state.isDeviceSecure,
        error = state.error,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
private fun AccountPreferenceContent(
    onBackClick: () -> Unit,
    onGetFreeXrdClick: () -> Unit,
    canUseFaucet: Boolean,
    loading: Boolean,
    isDeviceSecure: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
) {
    val showNotSecuredDialog = remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.account_preferences),
            onBackClick = onBackClick,
            containerColor = RadixTheme.colors.gray5
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.gray5)
                    .padding(RadixTheme.dimensions.paddingLarge)

            ) {
                val context = LocalContext.current
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.get_free_xrd),
                    onClick = {
                        if (isDeviceSecure) {
                            context.findFragmentActivity()?.let { activity ->
                                activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                    if (authenticatedSuccessfully) {
                                        onGetFreeXrdClick()
                                    }
                                }
                            }
                        } else {
                            showNotSecuredDialog.value = true
                        }
                    },
                    enabled = !loading && canUseFaucet
                )
                if (loading) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                    Text(
                        text = stringResource(R.string.this_may_take_several),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1,
                    )
                }
            }
            if (loading) {
                FullscreenCircularProgressContent()
            }
            SnackbarUiMessageHandler(message = error) {
                onMessageShown()
            }
        }
        NotSecureAlertDialog(show = showNotSecuredDialog.value, finish = {
            showNotSecuredDialog.value = false
            if (it) {
                onGetFreeXrdClick()
            }
        })
    }
}

@Preview(showBackground = true)
@Composable
fun AccountPreferencePreview() {
    RadixWalletTheme {
        AccountPreferenceContent(
            onBackClick = {},
            onGetFreeXrdClick = {},
            canUseFaucet = true,
            loading = false,
            isDeviceSecure = true,
            onMessageShown = {},
            error = null
        )
    }
}
