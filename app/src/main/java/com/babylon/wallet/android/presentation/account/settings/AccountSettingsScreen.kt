package com.babylon.wallet.android.presentation.account.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.AccountHidden -> onHideAccountClick()
            }
        }
    }

    val hideSheetAction: () -> Unit = remember {
        {
            scope.launch {
                bottomSheetState.hide()
                viewModel.setBottomSheetContent(AccountPreferenceUiState.BottomSheetContent.None)
            }
        }
    }

    BackHandler(enabled = bottomSheetState.isVisible) {
        hideSheetAction()
    }

    AccountSettingsContent(
        modifier = modifier,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        account = state.account,
        onShowRenameAccountClick = {
            scope.launch {
                viewModel.setBottomSheetContent(AccountPreferenceUiState.BottomSheetContent.RenameAccount)
                bottomSheetState.show()
            }
        },
        settingsSections = state.settingsSections,
        onSettingClick = { item ->
            state.account?.address?.let { accountAddress ->
                onSettingItemClick(item, accountAddress)
            }
        },
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        faucetState = state.faucetState,
        isXrdLoading = state.isFreeXRDLoading,
        onHideAccount = {
            scope.launch {
                viewModel.setBottomSheetContent(AccountPreferenceUiState.BottomSheetContent.HideAccount)
                bottomSheetState.show()
            }
        }
    )

    if (state.isBottomSheetVisible) {
        DefaultModalSheetLayout(
            wrapContent = true,
            enableImePadding = true,
            sheetState = bottomSheetState,
            sheetContent = {
                when (state.bottomSheetContent) {
                    AccountPreferenceUiState.BottomSheetContent.RenameAccount -> {
                        RenameAccountSheet(
                            accountNameChanged = state.accountNameChanged,
                            onNewAccountNameChange = viewModel::onRenameAccountNameChange,
                            isNewNameValid = state.isNewNameValid,
                            isNewNameLengthMoreThanTheMaximum = state.isNewNameLengthMoreThanTheMaximum,
                            onRenameAccountNameClick = {
                                viewModel.onRenameAccountNameConfirm()
                                hideSheetAction()
                            },
                            onClose = hideSheetAction
                        )
                    }

                    AccountPreferenceUiState.BottomSheetContent.HideAccount -> {
                        HideAccountSheet(
                            onHideAccountClick = viewModel::onHideAccount,
                            onClose = hideSheetAction
                        )
                    }

                    AccountPreferenceUiState.BottomSheetContent.None -> {}
                }
            },
            showDragHandle = true,
            onDismissRequest = hideSheetAction
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
                windowInsets = WindowInsets.statusBarsAndBanner
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
            item {
                if (faucetState is FaucetState.Available) {
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
                        onClick = onGetFreeXrdClick,
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
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
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
    BottomSheet(
        modifier = modifier,
        onClose = onClose
    ) {
        Image(
            modifier = Modifier
                .size(51.dp)
                .align(Alignment.CenterHorizontally),
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_show),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.accountSettings_hideThisAccount),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.accountSettings_hideAccount_message),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RadixTheme.dimensions.paddingSemiLarge,
                    end = RadixTheme.dimensions.paddingSemiLarge,
                    bottom = RadixTheme.dimensions.paddingXXLarge
                )
        ) {
            RadixSecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.common_cancel),
                onClick = onClose
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            RadixPrimaryButton(
                modifier = Modifier.weight(1.5f),
                text = stringResource(id = R.string.accountSettings_hideAccount_button),
                onClick = onHideAccountClick
            )
        }
    }
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
