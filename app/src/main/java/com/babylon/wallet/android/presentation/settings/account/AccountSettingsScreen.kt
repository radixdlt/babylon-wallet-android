<<<<<<<< HEAD:app/src/main/java/com/babylon/wallet/android/presentation/account/accountpreference/AccountPreferencesScreen.kt
package com.babylon.wallet.android.presentation.account.accountpreference
========
package com.babylon.wallet.android.presentation.settings.account
>>>>>>>> 6c886c7c9 (third party deposits UI):app/src/main/java/com/babylon/wallet/android/presentation/settings/account/AccountSettingsScreen.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
<<<<<<<< HEAD:app/src/main/java/com/babylon/wallet/android/presentation/account/accountpreference/AccountPreferencesScreen.kt
import androidx.compose.foundation.layout.statusBars
========
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
>>>>>>>> 6c886c7c9 (third party deposits UI):app/src/main/java/com/babylon/wallet/android/presentation/settings/account/AccountSettingsScreen.kt
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
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingClick: (AccountSettingItem, String) -> Unit
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
                        .background(
                            color = RadixTheme.colors.defaultBackground,
                            shape = RadixTheme.shapes.roundedRectTopDefault
                        )
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
        AccountSettingsContent(
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
            },
            settingsSections = state.settingsSections,
            onSettingClick = {
                onSettingClick(it, state.accountAddress)
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
private fun AccountSettingsContent(
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
    settingsSections: ImmutableList<AccountSettingsSection>,
    onSettingClick: (AccountSettingItem) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = error,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    Scaffold(
        modifier = modifier,
<<<<<<<< HEAD:app/src/main/java/com/babylon/wallet/android/presentation/account/accountpreference/AccountPreferencesScreen.kt
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
========
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.accountSettings_title),
            onBackClick = onBackClick,
            containerColor = RadixTheme.colors.defaultBackground
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val context = LocalContext.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.gray5)
            ) {
                settingsSections.forEach { section ->
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            text = stringResource(id = section.titleRes()),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray2
                        )
                    }
                    val lastSettingsItem = section.settingsItems.last()
                    section.settingsItems.forEach { settingsItem ->
                        item {
                            DefaultSettingsItem(
                                onClick = {
                                    onSettingClick(settingsItem)
                                },
                                icon = settingsItem.getIcon(),
                                title = stringResource(id = settingsItem.titleRes()),
                                subtitle = stringResource(id = settingsItem.subtitleRes())
                            )
                            if (lastSettingsItem != settingsItem) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                    color = RadixTheme.colors.gray5
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
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
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                }
                if (!hasAuthKey) {
                    item {
                        RadixSecondaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "Create &amp; Upload Auth Key",
                            onClick = onCreateAndUploadAuthKey,
                            enabled = !loading,
                            throttleClicks = true
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    }
                }
                item {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.addressAction_showAccountQR),
                        onClick = onShowQRCodeClick,
                        enabled = !loading
                    )
                }

                if (loading) {
                    item {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
                        Text(
                            text = stringResource(R.string.accountSettings_loadingPrompt),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1,
                        )
                    }
                }
>>>>>>>> 6c886c7c9 (third party deposits UI):app/src/main/java/com/babylon/wallet/android/presentation/settings/account/AccountSettingsScreen.kt
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
fun AccountSettingsPreview() {
    RadixWalletTheme {
        AccountSettingsContent(
            onBackClick = {},
            onGetFreeXrdClick = {},
            onShowQRCodeClick = {},
            faucetState = FaucetState.Available(isEnabled = true),
            isXrdLoading = false,
            isAuthSigningLoading = false,
            onMessageShown = {},
            error = null,
            hasAuthKey = false,
            onCreateAndUploadAuthKey = {},
            settingsSections = persistentListOf(
                AccountSettingsSection.AccountSection(
                    listOf(AccountSettingItem.ThirdPartyDeposits)
                )
            ),
            onSettingClick = {}
        )
    }
}
