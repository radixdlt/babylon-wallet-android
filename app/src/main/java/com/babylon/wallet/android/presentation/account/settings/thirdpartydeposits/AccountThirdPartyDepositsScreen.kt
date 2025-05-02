package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
fun AccountThirdPartyDepositsScreen(
    viewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAssetSpecificRulesClick: (AccountAddress) -> Unit,
    onSpecificDepositorsClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCancelPrompt by remember { mutableStateOf(false) }
    val backClick = {
        if (state.canUpdate) {
            showCancelPrompt = true
        } else {
            onBackClick()
        }
    }
    BackHandler {
        backClick()
    }
    if (showCancelPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onBackClick()
                }
                showCancelPrompt = false
            },
            message = {
                Text(
                    text = stringResource(
                        R.string.accountSettings_thirdPartyDeposits_discardMessage
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(
                id = R.string.accountSettings_thirdPartyDeposits_discardChanges
            ),
            dismissText = stringResource(
                id = R.string.accountSettings_thirdPartyDeposits_keepEditing
            ),
            confirmTextColor = RadixTheme.colors.error
        )
    }
    AccountThirdPartyDepositsContent(
        onBackClick = backClick,
        canUpdate = state.canUpdate,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onAllowAll = viewModel::onAllowAll,
        onAcceptKnown = viewModel::onAcceptKnown,
        onDenyAll = viewModel::onDenyAll,
        onAssetSpecificRulesClick = { onAssetSpecificRulesClick(state.accountAddress) },
        accountThirdPartyDepositSettings = state.updatedThirdPartyDepositSettings,
        onUpdateThirdPartyDeposits = viewModel::onUpdateThirdPartyDeposits,
        onSpecificDepositorsClick = onSpecificDepositorsClick
    )
}

@Composable
private fun AccountThirdPartyDepositsContent(
    onBackClick: () -> Unit,
    canUpdate: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
    onAllowAll: () -> Unit,
    onAcceptKnown: () -> Unit,
    onDenyAll: () -> Unit,
    onAssetSpecificRulesClick: () -> Unit,
    accountThirdPartyDepositSettings: ThirdPartyDeposits?,
    onUpdateThirdPartyDeposits: () -> Unit,
    onSpecificDepositorsClick: () -> Unit
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
                title = stringResource(R.string.accountSettings_thirdPartyDeposits),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onUpdateThirdPartyDeposits,
                text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_update),
                enabled = canUpdate
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val accountDepositRule = accountThirdPartyDepositSettings?.depositRule
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountSettings_thirdPartyDeposits_text),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.textSecondary
            )
            DepositOptionItem(
                onClick = onAllowAll,
                currentRule = DepositRule.ACCEPT_ALL,
                selectedRule = accountDepositRule
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.divider
            )
            DepositOptionItem(
                onClick = onAcceptKnown,
                currentRule = DepositRule.ACCEPT_KNOWN,
                selectedRule = accountDepositRule
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.divider
            )
            DepositOptionItem(
                onClick = onDenyAll,
                currentRule = DepositRule.DENY_ALL,
                selectedRule = accountDepositRule,
                warning = stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAllWarning)
            )
            HorizontalDivider(
                color = RadixTheme.colors.divider
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingXLarge)
            )
            DepositOptionItem(
                onClick = onAssetSpecificRulesClick,
                title = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowDenySpecific),
                subtitle = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowDenySpecificSubtitle),
                trailingContent = {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.divider
            )
            DepositOptionItem(
                onClick = onSpecificDepositorsClick,
                title = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositors),
                subtitle = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositorsSubtitle),
                trailingContent = {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            )
            HorizontalDivider(
                color = RadixTheme.colors.divider
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingXXXXLarge)
            )
        }
    }
}

@Composable
fun getDepositRuleCopiesAndIcon(depositRule: DepositRule): Triple<String, String, Int> {
    return when (depositRule) {
        DepositRule.ACCEPT_ALL -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_acceptAll),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_acceptAllSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_accept_all,
            )
        }

        DepositRule.ACCEPT_KNOWN -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_onlyKnown),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_onlyKnownSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_accept_known,
            )
        }

        DepositRule.DENY_ALL -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAll),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAllSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_deny_all,
            )
        }
    }
}

@Composable
private fun DepositOptionItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    warning: String? = null,
    currentRule: DepositRule,
    selectedRule: DepositRule?
) {
    val titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(currentRule)
    DefaultSettingsItem(
        modifier = modifier,
        title = titleSubtitleAndIcon.first,
        subtitle = titleSubtitleAndIcon.second,
        onClick = onClick,
        warningView = warning?.let {
            {
                Text(
                    text = warning,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.warning
                )
            }
        },
        leadingIcon = {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = titleSubtitleAndIcon.third),
                contentDescription = null
            )
        },
        trailingIcon = {
            Box(
                modifier = Modifier.size(24.dp)
            ) {
                if (selectedRule == currentRule) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }
    )
}

@Composable
private fun DepositOptionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        warningView = null,
        leadingIcon = icon?.let {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = it),
                    contentDescription = null // TODO Theme
                )
            }
        },
        trailingIcon = trailingContent
    )
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun AccountThirdPartyDepositsPreview() {
    RadixWalletTheme {
        AccountThirdPartyDepositsContent(
            onBackClick = {},
            canUpdate = false,
            onMessageShown = {},
            error = null,
            onAllowAll = {},
            onAcceptKnown = {},
            onDenyAll = {},
            onAssetSpecificRulesClick = {},
            accountThirdPartyDepositSettings = ThirdPartyDeposits.sample(),
            onUpdateThirdPartyDeposits = {},
            onSpecificDepositorsClick = {}
        )
    }
}
