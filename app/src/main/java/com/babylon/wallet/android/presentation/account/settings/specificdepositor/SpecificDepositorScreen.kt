package com.babylon.wallet.android.presentation.account.settings.specificdepositor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.UnknownDepositRulesStateInfo
import com.babylon.wallet.android.presentation.account.settings.specificassets.DeleteDialogState
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AssetType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleRandom
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.formatted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificDepositorScreen(
    sharedViewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by sharedViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val kb = LocalSoftwareKeyboardController.current
    val hideCallback = {
        kb?.hide()
        sharedViewModel.setAddDepositorSheetVisible(false)
        scope.launch { sheetState.hide() }
    }
    BackHandler {
        if (sheetState.isVisible) {
            hideCallback()
        } else {
            onBackClick()
        }
    }
    val dialogState = state.deleteDialogState
    if (dialogState is DeleteDialogState.AboutToDeleteAssetDepositor) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    sharedViewModel.onDeleteDepositor(dialogState.depositor)
                } else {
                    sharedViewModel.hideDeletePrompt()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeDepositor),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeDepositorMessage),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(
                id = R.string.common_remove
            ),
            dismissText = stringResource(
                id = R.string.common_cancel
            )
        )
    }

    SpecificDepositorContent(
        onBackClick = onBackClick,
        onMessageShown = sharedViewModel::onMessageShown,
        error = state.error,
        onShowAddAssetSheet = {
            sharedViewModel.setAddDepositorSheetVisible(true)
            scope.launch {
                sheetState.show()
            }
        },
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.gray5),
        allowedDepositors = state.allowedDepositorsUiModels,
        onDeleteDepositor = sharedViewModel::showDeletePrompt
    )

    if (state.isAddDepositorSheetVisible) {
        BottomSheetDialogWrapper(
            addScrim = true,
            showDragHandle = true,
            onDismiss = {
                hideCallback()
            },
            showDefaultTopBar = false
        ) {
            AddDepositorSheet(
                onResourceAddressChanged = sharedViewModel::depositorAddressTyped,
                onAddDepositor = {
                    hideCallback()
                    sharedViewModel.onAddDepositor()
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .clip(RadixTheme.shapes.roundedRectTopDefault),
                depositor = state.depositorToAdd,
                onDismiss = {
                    hideCallback()
                }
            )
        }
    }
}

@Composable
fun AddDepositorSheet(
    onResourceAddressChanged: (String) -> Unit,
    depositor: AssetType.DepositorType,
    onAddDepositor: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(
                rememberScrollState()
            )
            .imePadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            onDismissRequest = onDismiss
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountSettings_thirdPartyDeposits_addDepositorTitle),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.accountSettings_thirdPartyDeposits_addDepositorSubtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                onValueChanged = onResourceAddressChanged,
                value = depositor.addressToDisplay,
                hint = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetInputHint),
                singleLine = true,
                error = null
            )
            Spacer(modifier = Modifier.height(60.dp))
            RadixPrimaryButton(
                text = stringResource(R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositorsButton),
                onClick = {
                    onAddDepositor()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = depositor.addressValid,
                isLoading = false
            )
        }
    }
}

@Composable
private fun SpecificDepositorContent(
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    onShowAddAssetSheet: () -> Unit,
    modifier: Modifier = Modifier,
    allowedDepositors: ImmutableList<AssetType.DepositorType>?,
    onDeleteDepositor: (AssetType.DepositorType) -> Unit
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
                title = stringResource(R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositors),
                onBackClick = onBackClick,
                containerColor = RadixTheme.colors.defaultBackground,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
            ) {
                RadixPrimaryButton(
                    text = stringResource(R.string.accountSettings_thirdPartyDeposits_allowSpecificDepositorsButton),
                    onClick = onShowAddAssetSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    enabled = allowedDepositors != null
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                allowedDepositors == null -> UnknownDepositRulesStateInfo(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(RadixTheme.dimensions.paddingDefault)
                )

                allowedDepositors.isEmpty() -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_emptyAllowAll),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                            text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_allowInfo),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray2
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                        DepositorList(depositors = allowedDepositors.toPersistentList(), onDeleteDepositor = onDeleteDepositor)
                    }
                }
            }
        }
    }
}

@Composable
private fun DepositorList(
    depositors: ImmutableList<AssetType.DepositorType>,
    modifier: Modifier = Modifier,
    onDeleteDepositor: (AssetType.DepositorType) -> Unit
) {
    val lastItem = depositors.last()
    LazyColumn(modifier = modifier) {
        items(depositors) { depositor ->
            DepositorItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault),
                depositor = depositor,
                onDeleteDepositor = onDeleteDepositor
            )
            if (lastItem != depositor) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray5
                )
            }
        }
    }
}

@Composable
private fun DepositorItem(
    modifier: Modifier,
    depositor: AssetType.DepositorType,
    onDeleteDepositor: (AssetType.DepositorType) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        when (depositor.resource) {
            is Resource.FungibleResource -> Thumbnail.Fungible(token = depositor.resource, modifier = Modifier.size(44.dp))
            is Resource.NonFungibleResource -> Thumbnail.NonFungible(
                collection = depositor.resource,
                modifier = Modifier.size(44.dp)
            )

            else -> {}
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            depositor.resource?.name?.let {
                Text(
                    text = it.ifEmpty {
                        stringResource(id = R.string.authorizedDapps_dAppDetails_unknownTokenName)
                    },
                    maxLines = 1,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )
            }
            Text(
                text = remember(depositor.depositorAddress) {
                    depositor.depositorAddress?.formatted().orEmpty()
                },
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        IconButton(onClick = {
            onDeleteDepositor(depositor)
        }) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SpecificDepositorPreview() {
    RadixWalletTheme {
        val sampleDepositorResourceAddress = {
            AssetType.DepositorType(depositorAddress = ResourceOrNonFungible.Resource(ResourceAddress.sampleRandom(NetworkId.MAINNET)))
        }
        val sampleDepositorNftAddress = {
            AssetType.DepositorType(depositorAddress = ResourceOrNonFungible.NonFungible(NonFungibleGlobalId.sample()))
        }

        SpecificDepositorContent(
            onBackClick = {},
            onMessageShown = {},
            error = null,
            onShowAddAssetSheet = {},
            allowedDepositors = persistentListOf(
                sampleDepositorResourceAddress(),
                sampleDepositorNftAddress(),
                sampleDepositorResourceAddress()
            )
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun AddDepositorSheetPreview() {
    RadixWalletTheme {
        AddDepositorSheet(onResourceAddressChanged = {}, depositor = AssetType.DepositorType(), onAddDepositor = {}) {}
    }
}
