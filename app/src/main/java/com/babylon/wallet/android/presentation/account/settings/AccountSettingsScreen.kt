package com.babylon.wallet.android.presentation.account.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.Event
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.State
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.HideResourceSheetContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingItemClick: (AccountSettingItem, address: AccountAddress) -> Unit,
    onHideAccountClick: () -> Unit,
    onDeleteAccountClick: (AccountAddress) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.AccountHidden -> onHideAccountClick()
                is Event.OpenDeleteAccount -> onDeleteAccountClick(event.accountAddress)
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isBottomSheetVisible,
        onSheetClosed = viewModel::onDismissBottomSheet
    )

    AccountSettingsContent(
        modifier = modifier,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        account = state.account,
        onShowRenameAccountClick = viewModel::onRenameAccountRequest,
        settingsSections = state.settingsSections,
        onSettingClick = { item ->
            state.account?.address?.let { accountAddress ->
                onSettingItemClick(item, accountAddress)
            }
        },
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        faucetState = state.faucetState,
        isXrdLoading = state.isFreeXRDLoading,
        onHideAccount = viewModel::onHideAccountRequest,
        isAccountNameUpdated = state.isAccountNameUpdated,
        onSnackbarMessageShown = viewModel::onSnackbarMessageShown,
        onDeleteAccount = viewModel::onDeleteAccountRequest
    )

    if (state.isBottomSheetVisible) {
        DefaultModalSheetLayout(
            wrapContent = true,
            enableImePadding = true,
            sheetState = bottomSheetState,
            sheetContent = {
                when (val sheetState = state.bottomSheetContent) {
                    State.BottomSheetContent.RenameAccount -> {
                        RenameAccountSheet(
                            accountNameChanged = state.accountNameChanged,
                            onNewAccountNameChange = viewModel::onRenameAccountNameChange,
                            isNewNameValid = state.isNewNameValid,
                            isNewNameLengthMoreThanTheMaximum = state.isNewNameLengthMoreThanTheMaximum,
                            onRenameAccountNameClick = {
                                viewModel.onRenameAccountNameConfirm()
                            },
                            onClose = viewModel::onDismissBottomSheet
                        )
                    }

                    State.BottomSheetContent.HideAccount -> {
                        HideAccountSheet(
                            onHideAccountClick = viewModel::onHideAccount,
                            onClose = viewModel::onDismissBottomSheet
                        )
                    }

                    State.BottomSheetContent.None -> {}
                }
            },
            showDragHandle = true,
            onDismissRequest = viewModel::onDismissBottomSheet
        )
    }
}

@Composable
private fun AccountSettingsContent(
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    account: Account?,
    onShowRenameAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsSections: ImmutableList<AccountSettingsSection>,
    onSettingClick: (AccountSettingItem) -> Unit,
    onGetFreeXrdClick: () -> Unit,
    faucetState: FaucetState,
    isXrdLoading: Boolean,
    onHideAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    isAccountNameUpdated: Boolean,
    onSnackbarMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = error,
        snackbarHostState = snackbarHostState,
        onMessageShown = onMessageShown
    )

    val accountUpdatedText = stringResource(R.string.accountSettings_updatedAccountHUDMessage)
    LaunchedEffect(isAccountNameUpdated) {
        if (isAccountNameUpdated) {
            snackbarHostState.showSnackbar(
                message = accountUpdatedText,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            onSnackbarMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.accountSettings_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                if (faucetState is FaucetState.Available) {
                    RadixSecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.accountSettings_getXrdTestTokens),
                        onClick = onGetFreeXrdClick,
                        isLoading = isXrdLoading,
                        enabled = !isXrdLoading && faucetState.isEnabled
                    )
                    if (isXrdLoading) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                            text = stringResource(R.string.accountSettings_loadingPrompt),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }

                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.accountSettings_hideAccount_button),
                    onClick = onHideAccount
                )

                WarningButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.accountSettings_deleteAccount),
                    onClick = onDeleteAccount
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackbarHostState
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
                account?.let {
                    SimpleAccountCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .padding(top = RadixTheme.dimensions.paddingDefault),
                        account = account
                    )
                }
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
                            leadingIconRes = settingsItem.getIcon(),
                            title = stringResource(id = settingsItem.titleRes()),
                            subtitle = stringResource(id = settingsItem.subtitleRes())
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
    BottomSheet(
        modifier = modifier,
        onClose = onClose
    ) {
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
private fun HideAccountSheet(
    modifier: Modifier = Modifier,
    onHideAccountClick: () -> Unit,
    onClose: () -> Unit
) {
    HideResourceSheetContent(
        modifier = modifier,
        title = stringResource(id = R.string.accountSettings_hideThisAccount),
        description = stringResource(id = R.string.accountSettings_hideAccount_message),
        positiveButton = stringResource(id = R.string.accountSettings_hideAccount_button),
        onPositiveButtonClick = onHideAccountClick,
        onClose = onClose
    )
}

@Composable
private fun BottomSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.padding(
                start = RadixTheme.dimensions.paddingXXSmall,
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

        content()
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
            account = Account.sampleMainnet(),
            onShowRenameAccountClick = {},
            settingsSections = persistentListOf(
                AccountSettingsSection.AccountSection(
                    listOf(
                        AccountSettingItem.AccountLabel,
                        AccountSettingItem.ThirdPartyDeposits(DepositRule.ACCEPT_ALL)
                    )
                )
            ),
            onSettingClick = {},
            onGetFreeXrdClick = {},
            faucetState = FaucetState.Available(isEnabled = true),
            isXrdLoading = false,
            onHideAccount = {},
            onDeleteAccount = {},
            isAccountNameUpdated = false,
            onSnackbarMessageShown = {}
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

@Preview(showBackground = true)
@Composable
fun HideAccountSheetPreview() {
    RadixWalletTheme {
        HideAccountSheet(
            onHideAccountClick = {},
            onClose = {}
        )
    }
}
