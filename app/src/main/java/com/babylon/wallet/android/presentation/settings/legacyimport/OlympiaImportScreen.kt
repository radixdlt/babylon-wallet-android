@file:Suppress("CyclomaticComplexMethod")
@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)

package com.babylon.wallet.android.presentation.settings.legacyimport

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.AccountCardWithStack
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.olympiaimport.ChunkInfo
import rdx.works.profile.olympiaimport.OlympiaAccountDetails

@Composable
fun OlympiaImportScreen(
    viewModel: OlympiaImportViewModel,
    onCloseScreen: () -> Unit,
    modifier: Modifier = Modifier,
    onAddP2PLink: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OlympiaImportContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = viewModel::onBackClick,
        onQrCodeScanned = viewModel::onQrCodeScanned,
        pages = state.pages,
        oneOffEvent = viewModel.oneOffEvent,
        legacyAccountDetails = state.olympiaAccounts,
        onImportAccounts = viewModel::onImportAccounts,
        onCloseScreen = onCloseScreen,
        importButtonEnabled = state.importButtonEnabled,
        seedPhrase = state.seedPhrase,
        bip39Passphrase = state.bip39Passphrase,
        onSeedPhraseChanged = viewModel::onSeedPhraseChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        importSoftwareAccountsEnabled = state.importSoftwareAccountsEnabled,
        onImportSoftwareAccounts = viewModel::onImportSoftwareAccounts,
        uiMessage = state.uiMessage,
        onMessageShown = viewModel::onMessageShown,
        migratedAccounts = state.migratedAccounts,
        onContinue = onCloseScreen,
        currentPage = state.currentPage,
        qrChunkInfo = state.qrChunkInfo,
        onMnemonicAlreadyImported = viewModel::onMnemonicAlreadyImported,
        isDeviceSecure = state.isDeviceSecure,
        accountsLeft = state.hardwareAccountsLeftToImport,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        onAddP2PLink = onAddP2PLink,
        ledgerFactorSources = state.usedLedgerFactorSources,
        addLedgerSheetState = state.addLedgerSheetState,
        onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
        deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.description(),
        totalHardwareAccounts = state.totalHardwareAccounts
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OlympiaImportContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    pages: ImmutableList<ImportPage>,
    oneOffEvent: Flow<OlympiaImportEvent>,
    legacyAccountDetails: ImmutableList<OlympiaAccountDetails>,
    onImportAccounts: () -> Unit,
    onCloseScreen: () -> Unit,
    importButtonEnabled: Boolean,
    seedPhrase: String,
    bip39Passphrase: String,
    onSeedPhraseChanged: (String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    importSoftwareAccountsEnabled: Boolean,
    onImportSoftwareAccounts: () -> Unit,
    uiMessage: UiMessage?,
    onMessageShown: () -> Unit,
    migratedAccounts: ImmutableList<AccountItemUiModel>,
    onContinue: () -> Unit,
    currentPage: ImportPage,
    qrChunkInfo: ChunkInfo?,
    onMnemonicAlreadyImported: () -> Unit,
    isDeviceSecure: Boolean,
    accountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    onAddP2PLink: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    addLedgerSheetState: AddLedgerSheetState,
    onSendAddLedgerRequest: () -> Unit,
    deviceModel: String?,
    totalHardwareAccounts: Int
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    var cameraVisible by remember {
        mutableStateOf(false)
    }
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val closeSheetCallback = {
        scope.launch {
            bottomSheetState.hide()
        }
    }
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler {
        when {
            bottomSheetState.isVisible -> closeSheetCallback()
            currentPage == ImportPage.ImportComplete || currentPage == ImportPage.ScanQr -> onCloseScreen()
            else -> onBackClick()
        }
    }
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }
    CameraVisibilityEffect(pagerState = pagerState, pages = pages, onCameraVisibilityChanged = {
        cameraVisible = it
    })
    LaunchedEffect(pages) {
        oneOffEvent.collect { event ->
            when (event) {
                is OlympiaImportEvent.NextPage -> {
                    scope.launch {
                        pagerState.animateScrollToPage(pages.indexOf(event.page))
                    }
                }

                is OlympiaImportEvent.PreviousPage -> {
                    val page = event.page
                    if (page == null) {
                        onCloseScreen()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pages.indexOf(page))
                        }
                    }
                }

                OlympiaImportEvent.BiometricPrompt -> {
                    if (isDeviceSecure) {
                        context.biometricAuthenticate { authenticatedSuccessfully ->
                            if (authenticatedSuccessfully) {
                                onImportSoftwareAccounts()
                            }
                        }
                    } else {
                        showNotSecuredDialog = true
                    }
                }
                OlympiaImportEvent.UseLedger -> {
                    closeSheetCallback()
                }
            }
        }
    }
    if (showNotSecuredDialog) {
        NotSecureAlertDialog(finish = {
            showNotSecuredDialog = false
            if (it) {
                onImportSoftwareAccounts()
            }
        })
    }
    Box(modifier = modifier) {
        DefaultModalSheetLayout(
            modifier = Modifier.fillMaxSize(),
            sheetState = bottomSheetState,
            sheetContent = {
                AddLedgerBottomSheet(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    deviceModel = deviceModel,
                    onSendAddLedgerRequest = onSendAddLedgerRequest,
                    addLedgerSheetState = addLedgerSheetState,
                    onConfirmLedgerName = {
                        onConfirmLedgerName(it)
                        closeSheetCallback()
                    },
                    onSheetClose = { closeSheetCallback() },
                    waitingForLedgerResponse = waitingForLedgerResponse,
                    onAddP2PLink = onAddP2PLink
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.empty),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.gray1,
                    backIconType = if (currentPage == ImportPage.ImportComplete) BackIconType.None else BackIconType.Back
                )
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    pageCount = pages.size,
                    state = pagerState,
                    userScrollEnabled = false
                ) { page ->
                    when (pages[page]) {
                        ImportPage.ScanQr -> {
                            ScanQrPage(
                                cameraPermissionGranted = cameraPermissionState.status.isGranted,
                                onQrCodeScanned = onQrCodeScanned,
                                isVisible = cameraVisible,
                                modifier = Modifier.fillMaxSize(),
                                qrChunkInfo = qrChunkInfo
                            )
                        }

                        ImportPage.AccountList -> {
                            AccountListPage(
                                modifier = Modifier.fillMaxSize(),
                                olympiaAccounts = legacyAccountDetails,
                                onImportAccounts = onImportAccounts,
                                importButtonEnabled = importButtonEnabled,
                            )
                        }

                        ImportPage.MnemonicInput -> {
                            InputMnemonicPage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(RadixTheme.dimensions.paddingDefault),
                                seedPhrase = seedPhrase,
                                bip39Passphrase = bip39Passphrase,
                                onSeedPhraseChanged = onSeedPhraseChanged,
                                onPassphraseChanged = onPassphraseChanged,
                                importSoftwareAccountsEnabled = importSoftwareAccountsEnabled,
                                onImportSoftwareAccounts = onImportSoftwareAccounts,
                                onMnemonicAlreadyImported = onMnemonicAlreadyImported
                            )
                        }

                        ImportPage.HardwareAccount -> {
                            HardwareImportScreen(
                                Modifier
                                    .fillMaxSize()
                                    .padding(RadixTheme.dimensions.paddingDefault),
                                totalHardwareAccounts = totalHardwareAccounts,
                                accountsLeft = accountsLeft,
                                waitingForLedgerResponse = waitingForLedgerResponse,
                                ledgerFactorSources = ledgerFactorSources
                            ) {
                                scope.launch {
                                    bottomSheetState.show()
                                }
                            }
                        }

                        ImportPage.ImportComplete -> {
                            ImportCompletePage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(RadixTheme.dimensions.paddingDefault),
                                migratedAccounts = migratedAccounts,
                                onContinue = onContinue
                            )
                        }
                    }
                }
            }
        }
        SnackbarUiMessageHandler(message = uiMessage) {
            onMessageShown()
        }
    }
}

@Composable
private fun CameraVisibilityEffect(
    pagerState: PagerState,
    pages: ImmutableList<ImportPage>,
    onCameraVisibilityChanged: (Boolean) -> Unit
) {
    LaunchedEffect(pagerState, pages) {
        snapshotFlow {
            pagerState.currentPage == pages.indexOf(ImportPage.ScanQr)
        }.distinctUntilChanged().collect { visible ->
            onCameraVisibilityChanged(visible)
        }
    }
}

@Composable
private fun ScanQrPage(
    cameraPermissionGranted: Boolean,
    onQrCodeScanned: (String) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    qrChunkInfo: ChunkInfo?
) {
    Box(
        modifier = modifier
    ) {
        if (cameraPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.importLegacyWallet_scanQRCode_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                qrChunkInfo?.let { chunkInfo ->
                    Text(
                        text = stringResource(
                            id = R.string.importOlympiaAccounts_scannedProgress,
                            chunkInfo.scanned,
                            chunkInfo.total
                        ),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }
                Text(
                    text = stringResource(id = R.string.importLegacyWallet_scanQRCodeInstructions),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                CameraPreview(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RadixTheme.shapes.roundedRectMedium),
                    disableBack = false,
                    isVisible = isVisible,
                    onQrCodeDetected = onQrCodeScanned
                )
            }
        }
    }
}

@Composable
private fun AccountListPage(
    modifier: Modifier = Modifier,
    olympiaAccounts: ImmutableList<OlympiaAccountDetails>,
    onImportAccounts: () -> Unit,
    importButtonEnabled: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.importLegacyWallet_accountsToImport_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.importLegacyWallet_accountsToImport_subtitle),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
            if (olympiaAccounts.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.importLegacyWallet_accountsToImport_emptyState),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            items(olympiaAccounts) { item ->
                val gradientColor = getAccountGradientColorsFor(item.index)
                LegacyAccountCard(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(gradientColor),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .applyIf(
                            condition = !item.alreadyImported,
                            modifier = Modifier.clip(RadixTheme.shapes.roundedRectSmall)
                        ),
                    accountName = item.accountName ?: stringResource(id = R.string.importLegacyWallet_unnamedAccount, item.index),
                    accountType = item.type,
                    address = item.address,
                    newAddress = item.newBabylonAddress
                )
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.importLegacyWallet_accountsToImport_button, olympiaAccounts.size),
            onClick = onImportAccounts,
            enabled = importButtonEnabled,
            throttleClicks = true
        )
    }
}

@Composable
private fun HardwareImportScreen(
    modifier: Modifier = Modifier,
    totalHardwareAccounts: Int,
    accountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onUseLedger: () -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.importLegacyWallet_hardwareImport_title, totalHardwareAccounts),
                    style = RadixTheme.typography.header,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = stringResource(id = R.string.importLegacyWallet_hardwareImport_subtitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                if (ledgerFactorSources.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectSmall)
                            .padding(RadixTheme.dimensions.paddingLarge),
                        text = stringResource(id = R.string.importLegacyWallet_hardwareImport_noLedgers),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.gray2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    AccountsLeftText(accountsLeft)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(
                            items = ledgerFactorSources,
                            key = { item ->
                                item.id.value
                            },
                            itemContent = { item ->
                                LedgerListItem(
                                    ledgerFactorSource = item,
                                    modifier = Modifier
                                        .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                                        .padding(RadixTheme.dimensions.paddingLarge),
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                            }
                        )
                        item {
                            AccountsLeftText(accountsLeft)
                        }
                    }
                }
            }
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                onClick = onUseLedger,
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger)
            )
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun AccountsLeftText(accountsLeft: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.importLegacyWallet_hardwareImport_footer, accountsLeft),
        style = RadixTheme.typography.body1Regular,
        color = RadixTheme.colors.gray1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ImportCompletePage(
    modifier: Modifier = Modifier,
    migratedAccounts: ImmutableList<AccountItemUiModel>,
    onContinue: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.importLegacyWallet_completion_title),
            maxLines = 1,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.importLegacyWallet_completion_subtitle),
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
            itemsIndexed(migratedAccounts) { index, item ->
                val gradientColor = getAccountGradientColorsFor(item.appearanceID)
                if (index == migratedAccounts.size - 1) {
                    AccountCardWithStack(Modifier.fillMaxWidth(0.8f), item.appearanceID, item.displayName.orEmpty(), item.address)
                } else {
                    SimpleAccountCard(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(
                                Brush.horizontalGradient(gradientColor),
                                RadixTheme.shapes.roundedRectSmall
                            )
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            ),
                        account = item,
                        vertical = true
                    )
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            item {
                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.importLegacyWallet_completion_footer),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.common_continue),
            onClick = onContinue,
            throttleClicks = true
        )
    }
}

@Composable
private fun InputMnemonicPage(
    modifier: Modifier = Modifier,
    seedPhrase: String,
    bip39Passphrase: String,
    onSeedPhraseChanged: (String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    importSoftwareAccountsEnabled: Boolean,
    onImportSoftwareAccounts: () -> Unit,
    onMnemonicAlreadyImported: () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)) {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = onSeedPhraseChanged,
            value = seedPhrase,
            leftLabel = stringResource(id = R.string.importOlympiaAccounts_seedPhrase),
            hint = stringResource(id = R.string.importOlympiaAccounts_seedPhrase),
        )
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = onPassphraseChanged,
            value = bip39Passphrase,
            leftLabel = stringResource(id = R.string.importOlympiaAccounts_bip39passphrase),
            hint = stringResource(id = R.string.importOlympiaAccounts_passphrase),
        )
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.importOlympiaAccounts_importLabel),
            onClick = onImportSoftwareAccounts,
            enabled = importSoftwareAccountsEnabled,
            throttleClicks = true
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.importOlympiaAccounts_alreadyImported),
            onClick = onMnemonicAlreadyImported,
            enabled = importSoftwareAccountsEnabled,
            throttleClicks = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenLinkConnectorWithoutActiveConnectorPreview() {
    RadixWalletTheme {
        OlympiaImportContent(
            onBackClick = {},
            onQrCodeScanned = {},
            pages = persistentListOf(ImportPage.ScanQr),
            oneOffEvent = flow {},
            legacyAccountDetails = persistentListOf(),
            onImportAccounts = {},
            onCloseScreen = {},
            importButtonEnabled = false,
            seedPhrase = "test",
            bip39Passphrase = "test",
            onSeedPhraseChanged = {},
            onPassphraseChanged = {},
            importSoftwareAccountsEnabled = false,
            onImportSoftwareAccounts = {},
            uiMessage = null,
            onMessageShown = {},
            migratedAccounts = persistentListOf(),
            onContinue = {},
            currentPage = ImportPage.ScanQr,
            qrChunkInfo = null,
            onMnemonicAlreadyImported = {},
            isDeviceSecure = true,
            accountsLeft = 5,
            waitingForLedgerResponse = false,
            onConfirmLedgerName = {},
            onAddP2PLink = {},
            ledgerFactorSources = persistentListOf(),
            addLedgerSheetState = AddLedgerSheetState.Connect,
            onSendAddLedgerRequest = {},
            deviceModel = null,
            totalHardwareAccounts = 2
        )
    }
}
