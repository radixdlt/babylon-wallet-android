package com.babylon.wallet.android.presentation.settings.preferences.gateways

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.forNetwork
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Composable
fun GatewaysScreen(
    modifier: Modifier = Modifier,
    viewModel: GatewaysViewModel,
    onCreateAccount: (NetworkId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GatewaysContent(
        modifier = modifier,
        state = state,
        onDeleteGateway = viewModel::onDeleteGateway,
        onGatewayClick = viewModel::onGatewayClick,
        onAddGatewayClick = { viewModel.setAddGatewaySheetVisible(true) },
        oneOffEvent = viewModel.oneOffEvent,
        onCreateAccount = onCreateAccount,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick
    )

    state.addGatewayInput?.let { input ->
        AddGatewaySheet(
            input = input,
            onAddGatewayClick = viewModel::onAddGateway,
            onUrlChanged = viewModel::onNewUrlChanged,
            onDismiss = { viewModel.setAddGatewaySheetVisible(false) }
        )
    }
}

@Composable
private fun GatewaysContent(
    modifier: Modifier = Modifier,
    state: GatewaysViewModel.State,
    onDeleteGateway: (GatewaysViewModel.State.GatewayUiItem) -> Unit,
    onGatewayClick: (Gateway) -> Unit,
    onAddGatewayClick: () -> Unit,
    oneOffEvent: Flow<GatewaysViewModel.Event>,
    onCreateAccount: (NetworkId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    LaunchedEffect(Unit) {
        oneOffEvent.collect {
            when (it) {
                is GatewaysViewModel.Event.CreateAccountOnNetwork -> {
                    onCreateAccount(it.networkId)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.gateways_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }

        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingMedium
                    ),
                    text = stringResource(id = R.string.gateways_subtitle),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.textSecondary
                )
                InfoButton(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                    text = stringResource(id = R.string.infoLink_title_gateways),
                    onClick = {
                        onInfoClick(GlossaryItem.gateways)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
            itemsIndexed(state.gatewayList) { index, gateway ->
                Column(
                    modifier = Modifier.background(color = RadixTheme.colors.cardOnSecondary)
                ) {
                    GatewayCard(
                        gateway = gateway,
                        onDeleteGateway = onDeleteGateway,
                        modifier = Modifier
                            .throttleClickable {
                                onGatewayClick(gateway.gateway)
                            }
                            .fillMaxWidth()
                            .padding(
                                start = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingSmall,
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault
                            )
                    )

                    if (remember(state.gatewayList.size) { index < state.gatewayList.size - 1 }) {
                        HorizontalDivider(
                            color = RadixTheme.colors.divider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        )
                    }
                }
            }
            item {
                HorizontalDivider(
                    color = RadixTheme.colors.divider,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.gateways_addNewGatewayButtonTitle),
                    onClick = onAddGatewayClick
                )
            }
        }
    }
}

@Composable
private fun AddGatewaySheet(
    input: GatewaysViewModel.State.AddGatewayInput,
    onAddGatewayClick: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    BottomSheetDialogWrapper(
        addScrim = true,
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.gateways_addNewGateway_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.gateways_addNewGateway_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .focusRequester(inputFocusRequester),
                onValueChanged = onUrlChanged,
                value = input.url,
                hint = stringResource(id = R.string.gateways_addNewGateway_textFieldPlaceholder),
                singleLine = true,
                error = when (input.failure) {
                    GatewaysViewModel.State.AddGatewayInput.Failure.AlreadyExist -> stringResource(
                        id = R.string.gateways_addNewGateway_errorDuplicateURL
                    )
                    GatewaysViewModel.State.AddGatewayInput.Failure.ErrorWhileAdding -> stringResource(
                        id = R.string.gateways_addNewGateway_establishingConnectionErrorMessage
                    )

                    else -> null
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        RadixBottomBar(
            onClick = onAddGatewayClick,
            text = stringResource(R.string.gateways_addNewGateway_addGatewayButtonTitle),
            enabled = input.isUrlValid,
            isLoading = input.isLoading,
            insets = WindowInsets.navigationBars.union(WindowInsets.ime)
        )
    }
}

@Composable
private fun GatewayCard(
    gateway: GatewaysViewModel.State.GatewayUiItem,
    onDeleteGateway: (GatewaysViewModel.State.GatewayUiItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var gatewayToDelete by remember { mutableStateOf<GatewaysViewModel.State.GatewayUiItem?>(null) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        if (gateway.selected) {
            Icon(
                modifier = Modifier.width(24.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )
        } else {
            Box(modifier = Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gateway.name(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = gateway.gateway.network.displayDescription,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!gateway.isWellKnown) {
            IconButton(onClick = {
                gatewayToDelete = gateway
            }) {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline
                    ),
                    tint = RadixTheme.colors.icon,
                    contentDescription = null
                )
            }
        }
    }
    if (gatewayToDelete != null) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    gatewayToDelete?.let(onDeleteGateway)
                }
                gatewayToDelete = null
            },
            title = {
                Text(
                    text = stringResource(id = R.string.gateways_removeGatewayAlert_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.text
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.gateways_removeGatewayAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(id = R.string.common_remove),
            confirmTextColor = RadixTheme.colors.error
        )
    }
}

@Composable
private fun GatewaysViewModel.State.GatewayUiItem.name(): String = if (isWellKnown) {
    when (gateway.network.id) {
        NetworkId.MAINNET -> stringResource(id = R.string.gateway_mainnet_title)
        NetworkId.STOKENET -> stringResource(id = R.string.gateway_stokenet_title)
        else -> gateway.network.displayDescription
    }
} else {
    url
}

@Preview(showBackground = true)
@Composable
private fun GatewaysScreenPreview() {
    RadixWalletTheme {
        GatewaysContent(
            state = GatewaysViewModel.State(
                currentGateway = Gateway.forNetwork(NetworkId.MAINNET),
                gatewayList = persistentListOf(
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.STOKENET),
                        selected = false
                    ),
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.MAINNET),
                        selected = true
                    ),
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.HAMMUNET),
                        selected = false
                    )
                )
            ),
            onAddGatewayClick = {},
            onDeleteGateway = {},
            onGatewayClick = {},
            oneOffEvent = flow { },
            onCreateAccount = {},
            onInfoClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddGatewaySheetPreview() {
    RadixWalletTheme {
        AddGatewaySheet(
            input = GatewaysViewModel.State.AddGatewayInput(),
            onAddGatewayClick = {},
            onUrlChanged = {},
            onDismiss = {}
        )
    }
}
