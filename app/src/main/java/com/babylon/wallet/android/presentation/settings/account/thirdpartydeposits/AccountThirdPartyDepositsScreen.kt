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
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.AccountQRCodeView
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountThirdPartyDepositsScreen(
    viewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAssetSpecificRulesClick: (String) -> Unit
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
        AccountThirdPartyDepositsContent(
            onBackClick = onBackClick,
            loading = state.isLoading,
            onMessageShown = viewModel::onMessageShown,
            error = state.error,
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.defaultBackground),
            onAllowAll = viewModel::onAllowAll,
            onAcceptKnown = viewModel::onAcceptKnown,
            onDenyAll = viewModel::onDenyAll,
            onAssetSpecificRulesClick = { onAssetSpecificRulesClick(state.accountAddress) },
            accountDepositRule = state.accountDepositRule
        )
    }
}

@Composable
private fun AccountThirdPartyDepositsContent(
    onBackClick: () -> Unit,
    loading: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    modifier: Modifier = Modifier,
    onAllowAll: () -> Unit,
    onAcceptKnown: () -> Unit,
    onDenyAll: () -> Unit,
    onAssetSpecificRulesClick: () -> Unit,
    accountDepositRule: Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule?
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.accountSettings_thirdPartyDeposits),
            onBackClick = onBackClick,
            containerColor = RadixTheme.colors.defaultBackground
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.gray5)
            ) {
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
                    selected = accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll
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
                    selected = accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
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
                    selected = accountDepositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll
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
                    subtitle = stringResource(id = R.string.accountSettings_thirdPartyDeposits_allowDenySpecificSubtitle)
                )
            }
            if (loading) {
                FullscreenCircularProgressContent()
            }
            SnackbarUiMessageHandler(message = error, onMessageShown = {
                onMessageShown()
            })
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
    selected: Boolean = false
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
        Column(modifier = Modifier.height(IntrinsicSize.Max), verticalArrangement = Arrangement.Center) {
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
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountThirdPartyDepositsPreview() {
    RadixWalletTheme {
        AccountThirdPartyDepositsContent(
            onBackClick = {},
            loading = false,
            onMessageShown = {},
            error = null,
            onAllowAll = {},
            onAcceptKnown = {},
            onDenyAll = {},
            onAssetSpecificRulesClick = {},
            accountDepositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
        )
    }
}
