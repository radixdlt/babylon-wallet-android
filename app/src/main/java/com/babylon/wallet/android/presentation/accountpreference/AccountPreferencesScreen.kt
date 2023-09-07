package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.AccountQRCodeView
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountPreferenceScreen(
    viewModel: AccountPreferenceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    ModalBottomSheetLayout(
        modifier = modifier,
        sheetContent = {
            Column(modifier = Modifier.navigationBarsPadding()) {
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
            faucetState = state.faucetState,
            isXrdLoading = state.isFreeXRDLoading,
            isAuthSigningLoading = state.isAuthSigningLoading,
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
    faucetState: FaucetState,
    isXrdLoading: Boolean,
    isAuthSigningLoading: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
    hasAuthKey: Boolean,
    onCreateAndUploadAuthKey: () -> Unit,
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
                title = stringResource(R.string.accountSettings_title),
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
        Column(
            Modifier
                .padding(padding)
                .padding(RadixTheme.dimensions.paddingLarge)
        ) {
            val context = LocalContext.current
            if (faucetState is FaucetState.Available) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                Text(
                    text = stringResource(R.string.accountSettings_loadingPrompt),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1,
                )
            }

            if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED && !hasAuthKey) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.biometrics_prompt_createSignAuthKey),
                    onClick = onCreateAndUploadAuthKey,
                    isLoading = isAuthSigningLoading,
                    enabled = !isAuthSigningLoading,
                    throttleClicks = true
                )
            }
            RadixSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.addressAction_showAccountQR),
                onClick = onShowQRCodeClick,
                enabled = !isXrdLoading
            )
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
