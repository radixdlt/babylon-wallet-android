package com.babylon.wallet.android.presentation.settings.preferences.gateways

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.string
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun GatewaysScreen(
    viewModel: GatewaysViewModel,
    onBackClick: () -> Unit,
    onCreateProfile: (Url, NetworkId) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GatewaysContent(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onAddGatewayClick = viewModel::onAddGateway,
        onNewUrlChanged = viewModel::onNewUrlChanged,
        onDeleteGateway = viewModel::onDeleteGateway,
        onGatewayClick = viewModel::onGatewayClick,
        oneOffEvent = viewModel.oneOffEvent,
        onCreateProfile = onCreateProfile,
        addGatewaySheetVisible = viewModel::setAddGatewaySheetVisible
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewaysContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onAddGatewayClick: () -> Unit,
    onNewUrlChanged: (String) -> Unit,
    onDeleteGateway: (GatewayWrapper) -> Unit,
    onGatewayClick: (Gateway) -> Unit,
    oneOffEvent: Flow<SettingsEditGatewayEvent>,
    onCreateProfile: (Url, NetworkId) -> Unit,
    addGatewaySheetVisible: (Boolean) -> Unit
) {
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = bottomSheetState.isVisible) {
        addGatewaySheetVisible(false)
        scope.launch {
            bottomSheetState.hide()
        }
    }
    LaunchedEffect(Unit) {
        oneOffEvent.collect {
            when (it) {
                is SettingsEditGatewayEvent.CreateProfileOnNetwork -> {
                    onCreateProfile(it.newUrl, it.networkId)
                }
                else -> {
                    addGatewaySheetVisible(false)
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.gateways_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.gateways_subtitle),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
//                    InfoLink( // TODO enable it when we have a link
//                        stringResource(R.string.gateways_whatIsAGateway),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
//                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    HorizontalDivider(
                        color = RadixTheme.colors.gray5,
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                }
                items(state.gatewayList) { gateway ->
                    GatewayCard(
                        gateway = gateway,
                        onDeleteGateway = onDeleteGateway,
                        modifier = Modifier
                            .throttleClickable {
                                onGatewayClick(gateway.gateway)
                            }
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault)
                    )
                    HorizontalDivider(
                        color = RadixTheme.colors.gray5,
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.gateways_addNewGatewayButtonTitle),
                        onClick = {
                            addGatewaySheetVisible(true)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (state.isAddGatewaySheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier,
            sheetState = bottomSheetState,
            wrapContent = true,
            enableImePadding = true,
            sheetContent = {
                AddGatewaySheet(
                    onAddGatewayClick = onAddGatewayClick,
                    newUrl = state.newUrl,
                    onNewUrlChanged = onNewUrlChanged,
                    onClose = {
                        addGatewaySheetVisible(false)
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    newUrlValid = state.newUrlValid,
                    addingGateway = state.addingGateway,
                    modifier = Modifier.navigationBarsPadding(),
                    gatewayAddFailure = state.gatewayAddFailure
                )
            },
            onDismissRequest = {
                addGatewaySheetVisible(false)
                scope.launch {
                    bottomSheetState.hide()
                }
            }
        )
    }
}

@Composable
private fun AddGatewaySheet(
    onAddGatewayClick: () -> Unit,
    newUrl: String,
    onNewUrlChanged: (String) -> Unit,
    onClose: () -> Unit,
    newUrlValid: Boolean,
    gatewayAddFailure: GatewayAddFailure?,
    addingGateway: Boolean,
    modifier: Modifier = Modifier
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
            text = stringResource(id = R.string.gateways_addNewGateway_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.gateways_addNewGateway_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            onValueChanged = onNewUrlChanged,
            value = newUrl,
            hint = stringResource(id = R.string.gateways_addNewGateway_textFieldPlaceholder),
            singleLine = true,
            error = when (gatewayAddFailure) {
                GatewayAddFailure.AlreadyExist -> stringResource(id = R.string.gateways_addNewGateway_errorDuplicateURL)
                GatewayAddFailure.ErrorWhileAdding -> stringResource(
                    id = R.string.gateways_addNewGateway_establishingConnectionErrorMessage
                )
                else -> null
            }
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        RadixPrimaryButton(
            text = stringResource(R.string.gateways_addNewGateway_addGatewayButtonTitle),
            onClick = {
                onAddGatewayClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
                .padding(bottom = RadixTheme.dimensions.paddingSemiLarge),
            enabled = newUrlValid,
            isLoading = addingGateway
        )
    }
}

@Composable
private fun GatewayCard(
    gateway: GatewayWrapper,
    onDeleteGateway: (GatewayWrapper) -> Unit,
    modifier: Modifier = Modifier
) {
    var gatewayToDelete by remember { mutableStateOf<GatewayWrapper?>(null) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        if (gateway.selected) {
            Icon(
                modifier = Modifier.width(24.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                contentDescription = null
            )
        } else {
            Box(modifier = Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gateway.gateway.displayName(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = gateway.gateway.network.displayDescription,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (gateway.canBeDeleted) {
            IconButton(onClick = {
                gatewayToDelete = gateway
            }) {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline
                    ),
                    tint = RadixTheme.colors.gray1,
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
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.gateways_removeGatewayAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.common_remove),
        )
    }
}

@Composable
private fun Gateway.displayName(): String = string

@Preview(showBackground = true)
@Composable
fun GatewaysScreenPreview() {
    RadixWalletTheme {
        GatewaysContent(
            state = SettingsUiState(
                currentGateway = Gateway.forNetwork(NetworkId.MAINNET),
                gatewayList = persistentListOf(
                    GatewayWrapper(
                        gateway = Gateway.forNetwork(NetworkId.STOKENET),
                        selected = false
                    ),
                    GatewayWrapper(
                        gateway = Gateway.forNetwork(NetworkId.MAINNET),
                        selected = true
                    )
                ),
                newUrl = "",
                newUrlValid = false,
                addingGateway = true,
                gatewayAddFailure = null
            ),
            onBackClick = {},
            onAddGatewayClick = {},
            onNewUrlChanged = {},
            onDeleteGateway = {},
            onGatewayClick = {},
            oneOffEvent = flow { },
            onCreateProfile = { _, _ -> },
            addGatewaySheetVisible = {}
        )
    }
}
