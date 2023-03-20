@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.editgateway

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
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
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Gateway

@Composable
fun SettingsEditGatewayScreen(
    viewModel: SettingsEditGatewayViewModel,
    onBackClick: () -> Unit,
    onCreateProfile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsEditGatewayContent(
        onBackClick = onBackClick,
        onSwitchToClick = viewModel::onAddGateway,
        newUrl = state.newUrl,
        onNewUrlChanged = viewModel::onNewUrlChanged,
        newUrlValid = state.newUrlValid,
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
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
private fun SettingsEditGatewayContent(
    onBackClick: () -> Unit,
    onSwitchToClick: () -> Unit,
    newUrl: String,
    onNewUrlChanged: (String) -> Unit,
    newUrlValid: Boolean,
    modifier: Modifier = Modifier,
    gatewayList: PersistentList<GatewayWrapper>,
    onDeleteGateway: (GatewayWrapper) -> Unit,
    addingGateway: Boolean,
    gatewayAddFailure: GatewayAddFailure?,
    onGatewayClick: (Gateway) -> Unit,
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
                onAddGateway = onSwitchToClick,
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
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                gatewayAddFailure = gatewayAddFailure
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.gateways),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
            )
            Divider(color = RadixTheme.colors.gray5)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.choose_the_gateway_your_wallet),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    InfoLink(
                        stringResource(R.string.what_is_a_gateway),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    )
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
                        text = stringResource(id = R.string.add_new_gateway),
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

@Composable
private fun AddGatewaySheet(
    onAddGateway: () -> Unit,
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
            text = stringResource(id = R.string.add_new_gateway),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.enter_a_gateway_url),
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
            hint = stringResource(id = R.string.enter_full_url),
            singleLine = true,
            error = when (gatewayAddFailure) {
                GatewayAddFailure.AlreadyExist -> stringResource(id = R.string.already_exist)
                GatewayAddFailure.ErrorWhileAdding -> stringResource(id = R.string.adding_gateway_failed)
                else -> null
            }
        )
        Spacer(modifier = Modifier.height(40.dp))
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.add_gateway),
            onClick = {
                onAddGateway()
            },
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
        if (!gateway.gateway.isDefault) {
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
                    text = stringResource(id = R.string.remove_gateway),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.you_will_no_longer_be_able),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.remove),
        )
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
            gatewayList = persistentListOf(),
            onDeleteGateway = {},
            addingGateway = false,
            gatewayAddFailure = null,
            onGatewayClick = {},
            oneOffEvent = flow { },
            onCreateProfile = { _, _ -> }
        )
    }
}
