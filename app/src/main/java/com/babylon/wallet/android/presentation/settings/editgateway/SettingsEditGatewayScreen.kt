package com.babylon.wallet.android.presentation.settings.editgateway

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler

@Composable
fun SettingsEditGatewayScreen(
    viewModel: SettingsEditGatewayViewModel,
    onBackClick: () -> Unit,
    onCreateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    SettingsEditGatewayContent(
        onBackClick = onBackClick,
        onSwitchToClick = viewModel::onSwitchToClick,
        newUrl = state.newUrl,
        onNewUrlChanged = viewModel::onNewUrlChanged,
        newUrlValid = state.newUrlValid,
        currentNetworkName = state.currentNetworkAndGateway?.network?.name.orEmpty(),
        currentNetworkId = state.currentNetworkAndGateway?.network?.id?.toString().orEmpty(),
        currentNetworkEndpoint = state.currentNetworkAndGateway?.gatewayAPIEndpointURL.orEmpty(),
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        message = state.uiMessage,
        onMessageShown = viewModel::onMessageShown
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is SettingsEditGatewayEvent.CreateProfileOnNetwork -> {
                    onCreateProfile(it.newUrl, it.networkName)
                }
            }
        }
    }
}

@Composable
private fun SettingsEditGatewayContent(
    onBackClick: () -> Unit,
    onSwitchToClick: () -> Unit,
    newUrl: String,
    onNewUrlChanged: (String) -> Unit,
    newUrlValid: Boolean,
    currentNetworkName: String,
    currentNetworkId: String,
    currentNetworkEndpoint: String,
    modifier: Modifier = Modifier,
    message: UiMessage?,
    onMessageShown: () -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.network_gateway),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
            )
            Divider(color = RadixTheme.colors.gray5)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Text(
                    text = stringResource(R.string.your_radix_wallet_is_linked),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Divider(color = RadixTheme.colors.gray5)
                CurrentNetworkDetails(
                    currentNetworkName = currentNetworkName,
                    currentNetworkId = currentNetworkId,
                    currentNetworkEndpoint = currentNetworkEndpoint,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.new_url),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
                RadixTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    value = newUrl,
                    onValueChanged = onNewUrlChanged,
                    hint = stringResource(R.string.gateway_api_hint)
                )
                Spacer(modifier = Modifier.weight(1f))
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    text = stringResource(id = R.string.switch_to),
                    onClick = onSwitchToClick,
                    enabled = newUrlValid
                )
            }
        }
        SnackbarUiMessageHandler(message = message, onMessageShown = onMessageShown, modifier = Modifier.imePadding())
    }
}

@Composable
private fun CurrentNetworkDetails(
    currentNetworkName: String,
    currentNetworkId: String,
    currentNetworkEndpoint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            text = stringResource(id = R.string.current),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = stringResource(id = R.string.network_name),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Text(
                    text = currentNetworkName,
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            }
            Column {
                Text(
                    text = stringResource(id = R.string.network_id),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Text(
                    text = currentNetworkId,
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            }
        }
        Column {
            Text(
                text = stringResource(id = R.string.gateway_api_endpoint),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Text(
                text = currentNetworkEndpoint,
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsEditGatewayPreview() {
    RadixWalletTheme {
        SettingsEditGatewayContent(
            onBackClick = {},
            onSwitchToClick = {},
            newUrl = "",
            onNewUrlChanged = {},
            newUrlValid = false,
            currentNetworkName = "Hammunet",
            currentNetworkId = "34",
            currentNetworkEndpoint = BuildConfig.GATEWAY_API_URL,
            message = null,
            onMessageShown = {}
        )
    }
}
