@file:Suppress("CyclomaticComplexMethod")
@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class,
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.LedgerSelector
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
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
        onAccountSelected = viewModel::onAccountSelected,
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
        onToggleSelectAll = viewModel::onToggleSelectAll,
        qrChunkInfo = state.qrChunkInfo,
        onMnemonicAlreadyImported = viewModel::onMnemonicAlreadyImported,
        isDeviceSecure = state.isDeviceSecure,
        onSkipRemainingHardwareAccounts = viewModel::onSkipRemainingHardwareAccounts,
        accountsLeft = state.hardwareAccountsLeftToImport,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        onSkipLedgerName = viewModel::onSkipLedgerName,
        hasP2pLinks = state.hasP2pLinks,
        onAddP2PLink = onAddP2PLink,
        ledgerFactorSources = state.ledgerFactorSources,
        addLedgerSheetState = state.addLedgerSheetState,
        onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
        selectedFactorSourceID = state.selectedFactorSourceID,
        onLedgerFactorSourceSelected = viewModel::onLedgerFactorSourceSelected,
        onUseLedger = viewModel::onUseLedger
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
    legacyAccountDetails: ImmutableList<Selectable<OlympiaAccountDetails>>,
    onAccountSelected: (Selectable<OlympiaAccountDetails>) -> Unit,
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
    onToggleSelectAll: () -> Unit,
    qrChunkInfo: ChunkInfo?,
    onMnemonicAlreadyImported: () -> Unit,
    isDeviceSecure: Boolean,
    onSkipRemainingHardwareAccounts: () -> Unit,
    accountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    addLedgerSheetState: AddLedgerSheetState,
    onSendAddLedgerRequest: () -> Unit,
    selectedFactorSourceID: FactorSource.ID?,
    onUseLedger: () -> Unit,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit
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
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onImportSoftwareAccounts()
                                }
                            }
                        }
                    } else {
                        showNotSecuredDialog = true
                    }
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
    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        sheetContent = {
            AddLedgerBottomSheet(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingDefault),
                onSendAddLedgerRequest = onSendAddLedgerRequest,
                addLedgerSheetState = addLedgerSheetState,
                onConfirmLedgerName = {
                    onConfirmLedgerName(it)
                    closeSheetCallback()
                },
                waitingForLedgerResponse = waitingForLedgerResponse
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.importLegacyWallet_title),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.gray1,
                    backIconType = if (currentPage == ImportPage.ImportComplete) BackIconType.None else BackIconType.Back,
                    actions = {
                        if (currentPage == ImportPage.AccountList) {
                            IconButton(onClick = onToggleSelectAll) {
                                Icon(
                                    painterResource(
                                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_done_all
                                    ),
                                    tint = RadixTheme.colors.gray1,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
                Divider(color = RadixTheme.colors.gray5)
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
                                onAccountSelected = onAccountSelected,
                                onImportAccounts = onImportAccounts,
                                importButtonEnabled = importButtonEnabled
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
                                accountsLeft = accountsLeft,
                                onSkipRemainingHardwareAccounts = onSkipRemainingHardwareAccounts,
                                waitingForLedgerResponse = waitingForLedgerResponse,
                                hasP2pLinks = hasP2pLinks,
                                onAddP2PLink = onAddP2PLink,
                                ledgerFactorSources = ledgerFactorSources,
                                selectedFactorSourceID = selectedFactorSourceID,
                                onAddNewLedger = {
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                onUseLedger = onUseLedger,
                                onLedgerFactorSourceSelected = onLedgerFactorSourceSelected
                            )
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
            SnackbarUiMessageHandler(message = uiMessage) {
                onMessageShown()
            }
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
                    text = stringResource(id = com.babylon.wallet.android.R.string.importLegacyWallet_scanQRCodeInstructions),
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
    olympiaAccounts: ImmutableList<Selectable<OlympiaAccountDetails>>,
    onAccountSelected: (Selectable<OlympiaAccountDetails>) -> Unit,
    onImportAccounts: () -> Unit,
    importButtonEnabled: Boolean
) {
    Column(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(olympiaAccounts) { item ->
                val gradientColor = getAccountGradientColorsFor(item.data.index)
                val backgroundModifier = if (item.data.alreadyImported) {
                    Modifier.background(
                        RadixTheme.colors.gray4,
                        shape = RadixTheme.shapes.roundedRectSmall
                    )
                } else {
                    Modifier.background(
                        Brush.horizontalGradient(gradientColor),
                        shape = RadixTheme.shapes.roundedRectSmall
                    )
                }
                LegacyAccountSelectionCard(
                    modifier = backgroundModifier
                        .applyIf(
                            condition = !item.data.alreadyImported,
                            modifier = Modifier
                                .clip(RadixTheme.shapes.roundedRectSmall)
                                .throttleClickable {
                                    onAccountSelected(item)
                                }
                        ),
                    accountName = item.data.accountName,
                    accountType = item.data.type.name,
                    checked = item.selected,
                    address = item.data.address,
                    path = item.data.derivationPath.path,
                    alreadyImported = item.data.alreadyImported
                )
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.importOlympiaAccounts_title),
            onClick = onImportAccounts,
            enabled = importButtonEnabled,
            throttleClicks = true
        )
    }
}

@Composable
private fun HardwareImportScreen(
    modifier: Modifier = Modifier,
    accountsLeft: Int,
    onSkipRemainingHardwareAccounts: () -> Unit,
    waitingForLedgerResponse: Boolean,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    selectedFactorSourceID: FactorSource.ID?,
    onAddNewLedger: () -> Unit,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit,
    onUseLedger: () -> Unit
) {
    var showNoP2pLinksDialog by remember {
        mutableStateOf(false)
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.importOlympiaLedgerAccounts_unverifiedAccountsLeft, accountsLeft, accountsLeft),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            if (ledgerFactorSources.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_subtitleNoLedgers),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                LedgerSelector(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    selectedLedgerFactorSourceID = selectedFactorSourceID,
                    ledgerFactorSources = ledgerFactorSources,
                    onLedgerFactorSourceSelected = onLedgerFactorSourceSelected
                )
            }
            Spacer(Modifier.weight(1f))
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                onClick = {
                    if (hasP2pLinks) {
                        onAddNewLedger()
                    } else {
                        showNoP2pLinksDialog = true
                    }
                },
                text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger)
            )
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                onClick = onUseLedger,
                text = stringResource(id = R.string.common_continue),
                enabled = hasP2pLinks && ledgerFactorSources.isNotEmpty()
            )
            RadixSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Skip remaining accounts", // TODO skip feature will be available on iOS, so maybe we can add a String to crowdin
                onClick = onSkipRemainingHardwareAccounts,
                enabled = !waitingForLedgerResponse
            )
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
        if (showNoP2pLinksDialog) {
            BasicPromptAlertDialog(
                finish = {
                    if (it) {
                        onAddP2PLink()
                    }
                    showNoP2pLinksDialog = false
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = R.string.common_continue)
            )
        }
    }
}

@Composable
private fun ImportCompletePage(
    modifier: Modifier = Modifier,
    migratedAccounts: ImmutableList<AccountItemUiModel>,
    onContinue: () -> Unit
) {
    Column(modifier = modifier) {
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
                    stringResource(
                        id = when (migratedAccounts.size) {
                            0 -> R.string.importLegacyWallet_completion_titleNoAccounts
                            1 -> R.string.importLegacyWallet_completion_titleOneAccount
                            else -> R.string.importLegacyWallet_completion_titleManyAccounts
                        },
                        migratedAccounts.size,
                        migratedAccounts.size
                    ),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            }
            items(migratedAccounts) { item ->
                val gradientColor = getAccountGradientColorsFor(item.appearanceID)
                SimpleAccountCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(gradientColor),
                            RadixTheme.shapes.roundedRectSmall
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
                        ),
                    account = item
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
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
            onAccountSelected = {},
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
            onToggleSelectAll = {},
            qrChunkInfo = null,
            onMnemonicAlreadyImported = {},
            isDeviceSecure = true,
            onSkipRemainingHardwareAccounts = {},
            accountsLeft = 5,
            waitingForLedgerResponse = false,
            onConfirmLedgerName = {},
            onSkipLedgerName = {},
            hasP2pLinks = true,
            onAddP2PLink = {},
            ledgerFactorSources = persistentListOf(),
            addLedgerSheetState = AddLedgerSheetState.Connect,
            onSendAddLedgerRequest = {},
            selectedFactorSourceID = null,
            onUseLedger = {}
        ) {}
    }
}
