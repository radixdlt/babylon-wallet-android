package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.AccountQRCodeView
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountPreferenceScreen(
    viewModel: AccountPreferenceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    ModalBottomSheetLayout(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding(),
        sheetContent = {
            Column {
                BottomDialogDragHandle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                        .padding(vertical = RadixTheme.dimensions.paddingSmall),
                    onDismissRequest = {
                        scope.launch { sheetState.hide() }
                    }
                )

                AccountQRCodeView(accountAddress = state.accountAddress)
            }
        },
        sheetState = sheetState,
        sheetBackgroundColor = RadixTheme.colors.defaultBackground,
        sheetShape = RadixTheme.shapes.roundedRectTopDefault
    ) {
        AccountPreferenceContent(
            onBackClick = onBackClick,
            onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
            onShowQRCodeClick = {
                scope.launch { sheetState.show() }
            },
            canUseFaucet = state.canUseFaucet,
            loading = state.isLoading,
            isDeviceSecure = state.isDeviceSecure,
            onMessageShown = viewModel::onMessageShown,
            error = state.error,
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.defaultBackground),
            hasAuthKey = state.hasAuthKey,
            onCreateAndUploadAuthKey = viewModel::onCreateAndUploadAuthKey
        )
        state.interactionState?.let {
            SigningStatusBottomDialog(
                modifier = Modifier.fillMaxHeight(0.8f),
                onDismissDialogClick = viewModel::onDismissSigning,
                interactionState = it
            )
        }
    }
}

@Composable
private fun AccountPreferenceContent(
    onBackClick: () -> Unit,
    onGetFreeXrdClick: () -> Unit,
    onShowQRCodeClick: () -> Unit,
    canUseFaucet: Boolean,
    loading: Boolean,
    isDeviceSecure: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.accountSettings_title),
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
                    text = stringResource(R.string.accountSettings_getXrdTestTokens),
                    onClick = {
                        if (isDeviceSecure) {
                            context.biometricAuthenticate { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onGetFreeXrdClick()
                                }
                            }
                        } else {
                            showNotSecuredDialog = true
                        }
                    },
                    enabled = !loading && canUseFaucet
                )
                if (!hasAuthKey) {
                    RadixSecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Create &amp; Upload Auth Key",
                        onClick = onCreateAndUploadAuthKey,
                        enabled = !loading,
                        throttleClicks = true
                    )
                }
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.addressAction_showAccountQR),
                    onClick = onShowQRCodeClick,
                    enabled = !loading
                )

                if (loading) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                    Text(
                        text = stringResource(R.string.accountSettings_loadingPrompt),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1,
                    )
                }
            }
            if (loading) {
                FullscreenCircularProgressContent()
            }
            SnackbarUiMessageHandler(
                message = error,
                onMessageShown = onMessageShown
            )
        }

        if (showNotSecuredDialog) {
            NotSecureAlertDialog(finish = {
                showNotSecuredDialog = false
                if (it) {
                    onGetFreeXrdClick()
                }
            })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountPreferencePreview() {
    RadixWalletTheme {
        AccountPreferenceContent(
            onBackClick = {},
            onGetFreeXrdClick = {},
            onShowQRCodeClick = {},
            canUseFaucet = true,
            loading = false,
            isDeviceSecure = true,
            onMessageShown = {},
            error = null,
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {}
        )
    }
}
