package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
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
                androidx.compose.material3.Text(
                    text = stringResource(
                        R.string.accountSettings_thirdPartyDeposits_discardMessage
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(
                id = R.string.accountSettings_thirdPartyDeposits_discardChanges
            ),
            dismissText = stringResource(
                id = R.string.accountSettings_thirdPartyDeposits_keepEditing
            )
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
                containerColor = RadixTheme.colors.defaultBackground,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            BottomPrimaryButton(
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
                depositRule = DepositRule.ACCEPT_ALL
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onAllowAll,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == DepositRule.ACCEPT_ALL) {
                    checkIcon
                } else {
                    {}
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray5
            )
            titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(
                depositRule = DepositRule.ACCEPT_KNOWN
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onAcceptKnown,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == DepositRule.ACCEPT_KNOWN) {
                    checkIcon
                } else {
                    {}
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                color = RadixTheme.colors.gray5
            )
            titleSubtitleAndIcon = getDepositRuleCopiesAndIcon(
                depositRule = DepositRule.DENY_ALL
            )
            DepositOptionItem(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                onClick = onDenyAll,
                icon = titleSubtitleAndIcon.third,
                title = titleSubtitleAndIcon.first,
                subtitle = titleSubtitleAndIcon.second,
                trailingContent = if (accountDepositRule == DepositRule.DENY_ALL) {
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
            Image(painter = painterResource(id = it), contentDescription = null)
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
