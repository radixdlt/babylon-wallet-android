package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun AccountThirdPartyDepositsScreen(
    viewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAssetSpecificRulesClick: (String) -> Unit,
    onSpecificDepositorsClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountThirdPartyDepositsContent(
        onBackClick = onBackClick,
        canUpdate = state.canUpdate,
        onMessageShown = viewModel::onMessageShown,
        error = state.error,
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
            .navigationBarsPadding(),
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
    accountThirdPartyDepositSettings: Network.Account.OnLedgerSettings.ThirdPartyDeposits?,
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
                containerColor = RadixTheme.colors.defaultBackground
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
            ) {
                Divider(color = RadixTheme.colors.gray5)
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_update),
                    onClick = onUpdateThirdPartyDeposits,
                    enabled = canUpdate
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val accountDepositRule = accountThirdPartyDepositSettings?.depositRule
            val checkIcon: @Composable () -> Unit = {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountSettings_thirdPartyDeposits_text),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            var titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(
                depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onAllowAll,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll) {
                    checkIcon
                } else {
                    {}
                }
            )
            Divider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray5
            )
            titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(
                depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onAcceptKnown,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown) {
                    checkIcon
                } else {
                    {}
                }
            )
            Divider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray5
            )
            titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(
                depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onDenyAll,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll) {
                    checkIcon
                } else {
                    {}
                },
                warning = stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAllWarning)
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingLarge)
                    .background(RadixTheme.colors.gray4)
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onAssetSpecificRulesClick,
                title = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowDenySpecific),
                subtitle = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowDenySpecificSubtitle),
                trailingContent = {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray1
                    )
                }
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingLarge)
                    .background(RadixTheme.colors.gray4)
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onSpecificDepositorsClick,
                title = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositors),
                subtitle = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositorsSubtitle),
                trailingContent = {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray1
                    )
                }
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RadixTheme.dimensions.paddingLarge)
                    .background(RadixTheme.colors.gray4)
            )
        }
    }
}

@Composable
fun getDepositRuleCopiesAndIcon(depositRule: Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule): Triple<String, String, Int> {
    return when (depositRule) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_acceptAll),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_acceptAllSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_accept_all,
            )
        }

        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_onlyKnown),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_onlyKnownSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_accept_all,
            )
        }

        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll -> {
            Triple(
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAll),
                stringResource(id = R.string.accountSettings_thirdPartyDeposits_denyAllSubtitle),
                com.babylon.wallet.android.designsystem.R.drawable.ic_deny_all,
            )
        }
    }
}

@Composable
fun DepositOptionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    warning: String? = null,
    @DrawableRes icon: Int? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        icon?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }
        Column(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = title,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
            warning?.let { warning ->
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = warning,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.orange1
                )
            }
        }
        Box(modifier = Modifier.size(24.dp)) {
            trailingContent()
        }
    }
}

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
            accountThirdPartyDepositSettings = Network.Account.OnLedgerSettings.ThirdPartyDeposits(),
            onUpdateThirdPartyDeposits = {},
            onSpecificDepositorsClick = {}
        )
    }
}
