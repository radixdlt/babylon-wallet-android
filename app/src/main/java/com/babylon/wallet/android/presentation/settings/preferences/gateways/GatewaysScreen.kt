package com.babylon.wallet.android.presentation.settings.preferences.gateways

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.extensions.forNetwork
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
    state: GatewaysViewModel.State,
    onBackClick: () -> Unit,
    onAddGatewayClick: () -> Unit,
    onNewUrlChanged: (String) -> Unit,
    onDeleteGateway: (GatewaysViewModel.State.GatewayUiItem) -> Unit,
    onGatewayClick: (Gateway) -> Unit,
    oneOffEvent: Flow<GatewaysViewModel.Event>,
    onCreateProfile: (Url, NetworkId) -> Unit,
    addGatewaySheetVisible: (Boolean) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                is GatewaysViewModel.Event.CreateProfileOnNetwork -> {
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
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.gateways_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = RadixTheme.colors.gray5),
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
//                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
//                    InfoLink( // TODO enable it when we have a link
//                        stringResource(R.string.gateways_whatIsAGateway),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
//                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
                }
                itemsIndexed(state.gatewayList) { index, gateway ->
                    Column(
                        modifier = Modifier.background(color = RadixTheme.colors.white)
                    ) {
                        GatewayCard(
                            gateway = gateway,
                            onDeleteGateway = onDeleteGateway,
                            modifier = Modifier
                                .throttleClickable {
                                    onGatewayClick(gateway.gateway)
                                }
                                .fillMaxWidth()
                                .background(color = RadixTheme.colors.white)
                                .padding(
                                    start = RadixTheme.dimensions.paddingDefault,
                                    end = RadixTheme.dimensions.paddingSmall,
                                    top = RadixTheme.dimensions.paddingDefault,
                                    bottom = RadixTheme.dimensions.paddingDefault
                                )
                        )

                        if (remember(state.gatewayList.size) { index < state.gatewayList.size - 1 }) {
                            HorizontalDivider(
                                color = RadixTheme.colors.gray4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider(
                        color = RadixTheme.colors.gray4,
                        modifier = Modifier.fillMaxWidth()
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
        BottomSheetDialogWrapper(
            addScrim = true,
            showDragHandle = true,
            onDismiss = {
                addGatewaySheetVisible(false)
                scope.launch {
                    bottomSheetState.hide()
                }
            },
            showDefaultTopBar = false,
        ) {
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
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                gatewayAddFailure = state.gatewayAddFailure
            )
        }
    }
}

@Composable
private fun AddGatewaySheet(
    onAddGatewayClick: () -> Unit,
    newUrl: String,
    onNewUrlChanged: (String) -> Unit,
    onClose: () -> Unit,
    newUrlValid: Boolean,
    gatewayAddFailure: GatewaysViewModel.State.GatewayAddFailure?,
    addingGateway: Boolean,
    modifier: Modifier = Modifier
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    Box {
        Column(
            modifier = modifier
                .padding(bottom = 88.dp)
                .verticalScroll(rememberScrollState()),
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
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.gateways_addNewGateway_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .focusRequester(inputFocusRequester),
                onValueChanged = onNewUrlChanged,
                value = newUrl,
                hint = stringResource(id = R.string.gateways_addNewGateway_textFieldPlaceholder),
                singleLine = true,
                error = when (gatewayAddFailure) {
                    GatewaysViewModel.State.GatewayAddFailure.AlreadyExist -> stringResource(
                        id = R.string.gateways_addNewGateway_errorDuplicateURL
                    )
                    GatewaysViewModel.State.GatewayAddFailure.ErrorWhileAdding -> stringResource(
                        id = R.string.gateways_addNewGateway_establishingConnectionErrorMessage
                    )

                    else -> null
                },
                hintColor = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        BottomPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            text = stringResource(R.string.gateways_addNewGateway_addGatewayButtonTitle),
            onClick = onAddGatewayClick,
            enabled = newUrlValid,
            isLoading = addingGateway
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
                contentDescription = null
            )
        } else {
            Box(modifier = Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gateway.name(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = gateway.description(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
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
private fun GatewaysViewModel.State.GatewayUiItem.name(): String = if (isWellKnown) {
    when (gateway.network.id) {
        NetworkId.MAINNET -> stringResource(id = R.string.gateway_mainnet_title)
        NetworkId.STOKENET -> stringResource(id = R.string.gateway_stokenet_title)
        else -> url
    }
} else {
    url
}

@Composable
private fun GatewaysViewModel.State.GatewayUiItem.description(): String = when (gateway.network.id) {
    NetworkId.MAINNET -> stringResource(id = R.string.gateway_mainnet_description)
    NetworkId.STOKENET -> stringResource(id = R.string.gateway_stokenet_description)
    else -> gateway.network.displayDescription
}

@Preview(showBackground = true)
@Composable
private fun GatewaysScreenPreview(
    @PreviewParameter(GatewaysPreviewProvider::class) state: GatewaysViewModel.State
) {
    RadixWalletTheme {
        GatewaysContent(
            state = state,
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

class GatewaysPreviewProvider : PreviewParameterProvider<GatewaysViewModel.State> {

    override val values: Sequence<GatewaysViewModel.State>
        get() = sequenceOf(
            GatewaysViewModel.State(
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
                ),
                newUrl = "",
                newUrlValid = false,
                addingGateway = true,
                gatewayAddFailure = null
            ),
            GatewaysViewModel.State(
                currentGateway = Gateway.forNetwork(NetworkId.MAINNET),
                gatewayList = persistentListOf(
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.STOKENET),
                        selected = false
                    ),
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.MAINNET),
                        selected = true
                    )
                ),
                newUrl = "",
                newUrlValid = false,
                addingGateway = true,
                gatewayAddFailure = null,
                isAddGatewaySheetVisible = true
            ),
            GatewaysViewModel.State(
                currentGateway = Gateway.forNetwork(NetworkId.MAINNET),
                gatewayList = persistentListOf(
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.STOKENET),
                        selected = false
                    ),
                    GatewaysViewModel.State.GatewayUiItem(
                        gateway = Gateway.forNetwork(NetworkId.MAINNET),
                        selected = true
                    )
                ),
                newUrl = "",
                newUrlValid = false,
                addingGateway = true,
                gatewayAddFailure = GatewaysViewModel.State.GatewayAddFailure.ErrorWhileAdding,
                isAddGatewaySheetVisible = true
            )
        )
}
