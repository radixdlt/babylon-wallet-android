@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.appsettings.gateways

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix

@Composable
fun GatewaysScreen(
    viewModel: GatewaysViewModel,
    onBackClick: () -> Unit,
    onCreateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GatewaysContent(
        modifier = modifier,
        onBackClick = onBackClick,
        onAddGatewayClick = viewModel::onAddGateway,
        newUrl = state.newUrl,
        onNewUrlChanged = viewModel::onNewUrlChanged,
        newUrlValid = state.newUrlValid,
        gatewayList = state.gatewayList,
        onDeleteGateway = viewModel::onDeleteGateway,
        addingGateway = state.addingGateway,
        gatewayAddFailure = state.gatewayAddFailure,
        onGatewayClick = viewModel::onGatewayClick,
        oneOffEvent = viewModel.oneOffEvent,
        onCreateProfile = onCreateProfile
    )
}

@Composable
private fun GatewaysContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onAddGatewayClick: () -> Unit,
    newUrl: String,
    onNewUrlChanged: (String) -> Unit,
    newUrlValid: Boolean,
    gatewayList: PersistentList<GatewayWrapper>,
    onDeleteGateway: (GatewayWrapper) -> Unit,
    addingGateway: Boolean,
    gatewayAddFailure: GatewayAddFailure?,
    onGatewayClick: (Radix.Gateway) -> Unit,
    onCreateProfile: (String, String) -> Unit,
    oneOffEvent: Flow<SettingsEditGatewayEvent>
) {
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
        }
    }
    LaunchedEffect(Unit) {
        oneOffEvent.collect {
            when (it) {
                is SettingsEditGatewayEvent.CreateProfileOnNetwork -> {
                    onCreateProfile(it.newUrl, it.networkName)
                }
                else -> {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            }
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        wrapContent = true,
        enableImePadding = true,
        sheetContent = {
            AddGatewaySheet(
                onAddGatewayClick = onAddGatewayClick,
                newUrl = newUrl,
                onNewUrlChanged = onNewUrlChanged,
                onClose = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                },
                newUrlValid = newUrlValid,
                addingGateway = addingGateway,
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .navigationBarsPadding(),
                gatewayAddFailure = gatewayAddFailure
            )
        }
    ) {
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
                Divider(color = RadixTheme.colors.gray5)
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
                        Divider(
                            color = RadixTheme.colors.gray5,
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        )
                    }
                    items(gatewayList) { gateway ->
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
                        Divider(
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
                                scope.launch {
                                    bottomSheetState.show()
                                }
                            }
                        )
                    }
                }
            }
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
    gatewayAddFailure: GatewayAddFailure?,
    addingGateway: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.gateways_addNewGateway_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.gateways_addNewGateway_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
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
        Spacer(modifier = Modifier.height(40.dp))
        RadixPrimaryButton(
            text = stringResource(R.string.gateways_addNewGateway_addGatewayButtonTitle),
            onClick = {
                onAddGatewayClick()
            },
            modifier = Modifier.fillMaxWidth(),
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
                text = gateway.gateway.displayDescription(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!gateway.gateway.isWellKnown) {
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
            text = {
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
private fun Radix.Gateway.displayName(): String = when (network.id) {
    Radix.Network.ansharnet.id -> stringResource(id = R.string.gateways_rcNetGateway)
    else -> url
}

@Preview(showBackground = true)
@Composable
fun GatewaysScreenPreview() {
    RadixWalletTheme {
        GatewaysContent(
            onBackClick = {},
            onAddGatewayClick = {},
            newUrl = "",
            onNewUrlChanged = {},
            newUrlValid = false,
            gatewayList = persistentListOf(
                GatewayWrapper(
                    gateway = Radix.Gateway(
                        url = "https://babylon-stokenet-gateway.radixdlt.com/",
                        Radix.Network.stokenet
                    ),
                    selected = true
                ),
                GatewayWrapper(
                    gateway = Radix.Gateway(
                        url = "https://mainnet.radixdlt.com/",
                        Radix.Network.mainnet
                    ),
                    selected = false
                )
            ),
            onDeleteGateway = {},
            addingGateway = true,
            gatewayAddFailure = null,
            onGatewayClick = {},
            oneOffEvent = flow { },
            onCreateProfile = { _, _ -> }
        )
    }
}
