package com.babylon.wallet.android.presentation.account.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.usecases.FaucetState
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.Event
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.State
import com.babylon.wallet.android.presentation.common.secured.SecuredWithUiData
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.HideResourceSheetContent
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingItemClick: (AccountSettingItem, address: AccountAddress) -> Unit,
    onHideAccountClick: () -> Unit,
    onDeleteAccountClick: (AccountAddress) -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit,
    onApplyShieldClick: (AddressOfAccountOrPersona) -> Unit,
    onShieldClick: (AddressOfAccountOrPersona) -> Unit,
    onTimedRecoveryClick: (AddressOfAccountOrPersona) -> Unit
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
        state = state,
        onBackClick = onBackClick,
        onMessageShown = viewModel::onMessageShown,
        onShowRenameAccountClick = viewModel::onRenameAccountRequest,
        onSettingClick = { item ->
            state.account?.address?.let { accountAddress ->
                onSettingItemClick(item, accountAddress)
            }
        },
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        onHideAccount = viewModel::onHideAccountRequest,
        onSnackbarMessageShown = viewModel::onSnackbarMessageShown,
        onDeleteAccount = viewModel::onDeleteAccountRequest,
        onFactorSourceCardClick = onFactorSourceCardClick,
        onApplyShieldClick = { onApplyShieldClick(state.address) },
        onShieldClick = { onShieldClick(state.address) },
        onTimedRecoveryClick = onTimedRecoveryClick
    )

    if (state.isBottomSheetVisible) {
        when (state.bottomSheetContent) {
            State.BottomSheetContent.HideAccount -> {
                HideAccountSheet(
                    sheetState = bottomSheetState,
                    onHideAccountClick = viewModel::onHideAccount,
                    onDismiss = viewModel::onDismissBottomSheet
                )
            }

            State.BottomSheetContent.RenameAccount -> {
                RenameBottomSheet(
                    sheetState = bottomSheetState,
                    renameInput = state.renameAccountInput,
                    titleRes = R.string.accountSettings_renameAccount_title,
                    subtitleRes = R.string.accountSettings_renameAccount_subtitle,
                    errorValidationMessageRes = R.string.error_accountLabel_missing,
                    errorTooLongNameMessageRes = R.string.error_accountLabel_tooLong,
                    onNameChange = viewModel::onRenameAccountNameChange,
                    onUpdateNameClick = viewModel::onRenameAccountNameConfirm,
                    onDismiss = viewModel::onDismissBottomSheet,
                )
            }

            State.BottomSheetContent.None -> {}
        }
    }
}

@Composable
private fun AccountSettingsContent(
    state: State,
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    onShowRenameAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingClick: (AccountSettingItem) -> Unit,
    onGetFreeXrdClick: () -> Unit,
    onHideAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit,
    onApplyShieldClick: () -> Unit,
    onShieldClick: () -> Unit,
    onTimedRecoveryClick: (AddressOfAccountOrPersona) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.error,
        snackbarHostState = snackbarHostState,
        onMessageShown = onMessageShown
    )

    val accountUpdatedText = stringResource(R.string.accountSettings_updatedAccountHUDMessage)
    LaunchedEffect(state.isAccountNameUpdated) {
        if (state.isAccountNameUpdated) {
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
            RadixBottomBar(
                additionalTopContent = {
                    if (state.faucetState is FaucetState.Available) {
                        RadixSecondaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = stringResource(R.string.accountSettings_getXrdTestTokens),
                            onClick = onGetFreeXrdClick,
                            isLoading = state.isFreeXRDLoading,
                            enabled = !state.isFreeXRDLoading && state.faucetState.isEnabled
                        )
                        if (state.isFreeXRDLoading) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                                text = stringResource(R.string.accountSettings_loadingPrompt),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.text
                            )
                        }
                    }
                },
                button = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = RadixTheme.dimensions.paddingDefault)
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.accountSettings_hideAccount_button),
                        onClick = onHideAccount
                    )
                },
                additionalBottomContent = {
                    WarningButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = RadixTheme.dimensions.paddingDefault)
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.accountSettings_deleteAccount),
                        onClick = onDeleteAccount
                    )
                }
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackbarHostState
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                state.account?.let {
                    SimpleAccountCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .padding(top = RadixTheme.dimensions.paddingDefault),
                        account = state.account
                    )
                }
            }

            state.securedWith?.let { securedWith ->
                item {
                    Row {
                        SectionTitle(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.common_securedWith)
                        )

                        if (state.canApplyShield) {
                            RadixTextButton(
                                text = stringResource(id = R.string.commonSecurityShields_applyShield),
                                onClick = onApplyShieldClick
                            )
                        }
                    }

                    when (securedWith) {
                        is SecuredWithUiData.Factor -> FactorSourceCardView(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .throttleClickable { onFactorSourceCardClick(securedWith.factorSourceCard.id) },
                            item = securedWith.factorSourceCard
                        )

                        is SecuredWithUiData.Shield -> DefaultSettingsItem(
                            onClick = onShieldClick,
                            title = stringResource(id = R.string.commonSecurityShields_securityShield),
                            subtitle = stringResource(id = R.string.commonSecurityShields_securityShieldDetails),
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(id = DSR.ic_entity_update_shield),
                                    contentDescription = null,
                                    tint = RadixTheme.colors.icon
                                )
                            },
                            warningView = if (securedWith.isInTimedRecovery) {
                                {
                                    PromptLabel(
                                        modifier = Modifier.noIndicationClickable {
                                            onTimedRecoveryClick(state.address)
                                        },
                                        text = "Timed Recovery" // TODO crowdin
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
            state.settingsSections.forEach { section ->
                item {
                    SectionTitle(
                        title = stringResource(id = section.titleRes())
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
                                color = RadixTheme.colors.divider
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
        text = title,
        style = RadixTheme.typography.body1HighImportance,
        color = RadixTheme.colors.textSecondary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HideAccountSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    onHideAccountClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultModalSheetLayout(
        wrapContent = true,
        enableImePadding = true,
        sheetState = sheetState,
        sheetContent = {
            HideResourceSheetContent(
                modifier = modifier,
                title = stringResource(id = R.string.accountSettings_hideThisAccount),
                description = stringResource(id = R.string.accountSettings_hideAccount_message),
                positiveButton = stringResource(id = R.string.accountSettings_hideAccount_button),
                onPositiveButtonClick = onHideAccountClick,
                onClose = onDismiss
            )
        },
        showDragHandle = true,
        onDismissRequest = onDismiss
    )
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun AccountSettingsPreview(
    @PreviewParameter(AccountSettingsPreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme {
        AccountSettingsContent(
            state = state,
            onBackClick = {},
            onMessageShown = {},
            onShowRenameAccountClick = {},
            onSettingClick = {},
            onGetFreeXrdClick = {},
            onHideAccount = {},
            onDeleteAccount = {},
            onSnackbarMessageShown = {},
            onFactorSourceCardClick = {},
            onApplyShieldClick = {},
            onShieldClick = {},
            onTimedRecoveryClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HideAccountSheetPreview() {
    RadixWalletPreviewTheme {
        HideAccountSheet(
            sheetState = rememberModalBottomSheetState(),
            onHideAccountClick = {},
            onDismiss = {}
        )
    }
}

@UsesSampleValues
class AccountSettingsPreviewProvider : PreviewParameterProvider<State> {

    override val values: Sequence<State>
        get() = sequenceOf(
            State(
                error = null,
                account = Account.sampleMainnet(),
                settingsSections = persistentListOf(
                    AccountSettingsSection.AccountSection(
                        listOf(
                            AccountSettingItem.AccountLabel,
                            AccountSettingItem.ThirdPartyDeposits(DepositRule.ACCEPT_ALL)
                        )
                    )
                ),
                faucetState = FaucetState.Available(isEnabled = true),
                isFreeXRDLoading = false,
                isAccountNameUpdated = false,
                securedWith = SecuredWithUiData.Shield(
                    isInTimedRecovery = true
                )
            ),
            State(
                error = null,
                account = Account.sampleMainnet(),
                settingsSections = persistentListOf(
                    AccountSettingsSection.AccountSection(
                        listOf(
                            AccountSettingItem.AccountLabel,
                            AccountSettingItem.ThirdPartyDeposits(DepositRule.ACCEPT_ALL)
                        )
                    )
                ),
                faucetState = FaucetState.Available(isEnabled = true),
                isFreeXRDLoading = false,
                isAccountNameUpdated = false,
                securedWith = SecuredWithUiData.Factor(
                    factorSourceCard = DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                        includeLastUsedOn = true
                    )
                )
            )
        )
}
