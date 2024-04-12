package com.babylon.wallet.android.presentation.account.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.getDepositRuleCopiesAndIcon
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.AccountQRCodeView
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingItemClick: (AccountSettingItem, address: AccountAddress) -> Unit,
    onHideAccountClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showHideAccountPrompt by remember { mutableStateOf(false) }
    if (showHideAccountPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    viewModel.onHideAccount()
                }
                showHideAccountPrompt = false
            },
            title = {
                Text(
                    text = stringResource(id = R.string.accountSettings_hideThisAccount),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.accountSettings_hideAccountConfirmation),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.common_continue)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.AccountHidden -> onHideAccountClick()
            }
        }
    }
    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
            viewModel.resetBottomSheetContent()
        }
    }

    AccountSettingsContent(
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        accountName = state.accountName,
        onShowRenameAccountClick = {
            scope.launch {
                viewModel.setBottomSheetContentToRenameAccount()
                bottomSheetState.show()
            }
        },
        onShowAddressQRCodeClick = {
            scope.launch {
                viewModel.setBottomSheetContentToAddressQRCode()
                bottomSheetState.show()
            }
        },
        modifier = Modifier.navigationBarsPadding(),
        settingsSections = state.settingsSections,
        onSettingClick = {
            onSettingItemClick(it, state.accountAddress)
        },
        accountAddress = state.accountAddress,
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        faucetState = state.faucetState,
        isXrdLoading = state.isFreeXRDLoading,
        onHideAccount = {
            showHideAccountPrompt = true
        }
    )

    if (state.isBottomSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier,
            wrapContent = true,
            enableImePadding = true,
            sheetState = bottomSheetState,
            sheetContent = {
                when (state.bottomSheetContent) {
                    AccountPreferenceUiState.BottomSheetContent.RenameAccount -> {
                        RenameAccountSheet(
                            modifier = Modifier.navigationBarsPadding(),
                            accountNameChanged = state.accountNameChanged,
                            onNewAccountNameChange = viewModel::onRenameAccountNameChange,
                            isNewNameValid = state.isNewNameValid,
                            isNewNameLengthMoreThanTheMaximum = state.isNewNameLengthMoreThanTheMaximum,
                            onRenameAccountNameClick = {
                                viewModel.onRenameAccountNameConfirm()
                                scope.launch {
                                    bottomSheetState.hide()
                                    viewModel.resetBottomSheetContent()
                                }
                            },
                            onClose = {
                                scope.launch {
                                    bottomSheetState.hide()
                                    viewModel.resetBottomSheetContent()
                                }
                            }
                        )
                    }

                    AccountPreferenceUiState.BottomSheetContent.AddressQRCode -> {
                        AddressQRCodeSheet(
                            accountAddress = state.accountAddress,
                            dismissAddressQRCodeSheet = {
                                scope.launch {
                                    bottomSheetState.hide()
                                    viewModel.resetBottomSheetContent()
                                }
                            }
                        )
                    }

                    AccountPreferenceUiState.BottomSheetContent.None -> {}
                }
            },
            showDragHandle = state.bottomSheetContent == AccountPreferenceUiState.BottomSheetContent.AddressQRCode,
            onDismissRequest = {
                scope.launch {
                    bottomSheetState.hide()
                    viewModel.resetBottomSheetContent()
                }
            }
        )
    }
}

@Composable
private fun AccountSettingsContent(
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    accountName: String,
    onShowRenameAccountClick: () -> Unit,
    onShowAddressQRCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsSections: ImmutableList<AccountSettingsSection>,
    onSettingClick: (AccountSettingItem) -> Unit,
    accountAddress: AccountAddress,
    onGetFreeXrdClick: () -> Unit,
    faucetState: FaucetState,
    isXrdLoading: Boolean,
    onHideAccount: () -> Unit
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
        ) {
            item {
                ActionableAddressView(
                    address = Address.Account(accountAddress),
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                    textStyle = RadixTheme.typography.body2Regular,
                    textColor = RadixTheme.colors.gray2,
                    truncateAddress = false
                )
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(R.string.addressAction_showAccountQR),
                    onClick = onShowAddressQRCodeClick
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
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
                                if (settingsItem == AccountSettingItem.AccountLabel) {
                                    onShowRenameAccountClick()
                                } else {
                                    onSettingClick(settingsItem)
                                }
                            },
                            icon = settingsItem.getIcon(),
                            title = stringResource(id = settingsItem.titleRes()),
                            subtitle = when (settingsItem) {
                                AccountSettingItem.AccountLabel -> {
                                    accountName
                                }
                                is AccountSettingItem.ThirdPartyDeposits -> {
                                    getDepositRuleCopiesAndIcon(depositRule = settingsItem.defaultDepositRule).first
                                }
                                else -> {
                                    stringResource(id = settingsItem.subtitleRes())
                                }
                            }
                        )
                        if (lastSettingsItem != settingsItem) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                color = RadixTheme.colors.gray5
                            )
                        }
                    }
                }
            }
            item {
                if (faucetState is FaucetState.Available) {
                    val context = LocalContext.current

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = RadixTheme.dimensions.paddingLarge,
                                end = RadixTheme.dimensions.paddingLarge,
                                top = RadixTheme.dimensions.paddingDefault
                            ),
                        text = stringResource(R.string.accountSettings_getXrdTestTokens),
                        onClick = {
                            context.biometricAuthenticate { result ->
                                if (result == BiometricAuthenticationResult.Succeeded) {
                                    onGetFreeXrdClick()
                                }
                            }
                        },
                        isLoading = isXrdLoading,
                        enabled = !isXrdLoading && faucetState.isEnabled
                    )

                    if (isXrdLoading) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingXXXXLarge,
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                            text = stringResource(R.string.accountSettings_loadingPrompt),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1
                        )
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                } else {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
            item {
                WarningButton(
                    modifier = Modifier.padding(
                        start = RadixTheme.dimensions.paddingLarge,
                        end = RadixTheme.dimensions.paddingLarge,
                        bottom = RadixTheme.dimensions.paddingDefault
                    ),
                    text = stringResource(R.string.accountSettings_hideAccount_button),
                    onClick = onHideAccount
                )
            }
        }
    }
}

@Composable
private fun RenameAccountSheet(
    modifier: Modifier = Modifier,
    accountNameChanged: String,
    onNewAccountNameChange: (String) -> Unit,
    isNewNameValid: Boolean,
    isNewNameLengthMoreThanTheMaximum: Boolean,
    onRenameAccountNameClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.padding(
                start = RadixTheme.dimensions.paddingXSmall,
                top = RadixTheme.dimensions.paddingMedium
            ),
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.accountSettings_renameAccount_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.accountSettings_renameAccount_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            onValueChanged = onNewAccountNameChange,
            value = accountNameChanged,
            singleLine = true,
            error = if (isNewNameLengthMoreThanTheMaximum) {
                stringResource(id = R.string.error_accountLabel_tooLong)
            } else {
                null
            }
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
                .padding(bottom = RadixTheme.dimensions.paddingSemiLarge),
            text = stringResource(id = R.string.accountSettings_renameAccount_button),
            onClick = {
                onRenameAccountNameClick()
            },
            enabled = isNewNameValid
        )
    }
}

@Composable
private fun AddressQRCodeSheet(
    accountAddress: AccountAddress,
    dismissAddressQRCodeSheet: () -> Unit
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.defaultBackground,
                    shape = RadixTheme.shapes.roundedRectTopDefault
                ),
            onDismissRequest = {
                dismissAddressQRCodeSheet()
            }
        )

        AccountQRCodeView(accountAddress = accountAddress)
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun AccountSettingsPreview() {
    RadixWalletTheme {
        AccountSettingsContent(
            onBackClick = {},
            onMessageShown = {},
            error = null,
            accountName = "my cool account",
            onShowRenameAccountClick = {},
            onShowAddressQRCodeClick = {},
            settingsSections = persistentListOf(
                AccountSettingsSection.AccountSection(
                    listOf(
                        AccountSettingItem.AccountLabel,
                        AccountSettingItem.ThirdPartyDeposits(
                            Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll
                        )
                    )
                )
            ),
            onSettingClick = {},
            accountAddress = AccountAddress.sampleMainnet.random(),
            onGetFreeXrdClick = {},
            faucetState = FaucetState.Available(isEnabled = true),
            isXrdLoading = false,
            onHideAccount = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RenameAccountSheetPreview() {
    RadixWalletTheme {
        RenameAccountSheet(
            accountNameChanged = "updated",
            isNewNameValid = true,
            isNewNameLengthMoreThanTheMaximum = false,
            onNewAccountNameChange = {},
            onRenameAccountNameClick = {},
            onClose = {}
        )
    }
}
