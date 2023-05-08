@file:Suppress("CyclomaticComplexMethod")
@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class
)

package com.babylon.wallet.android.presentation.settings.legacyimport

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.pluralStringResource
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
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
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
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
        onHardwareImport = viewModel::onHardwareImport,
        currentPage = state.currentPage,
        onToggleSelectAll = viewModel::onToggleSelectAll,
        qrChunkInfo = state.qrChunkInfo,
        onMnemonicAlreadyImported = viewModel::onMnemonicAlreadyImported,
        isDeviceSecure = state.isDeviceSecure,
        onSkipRemainingHardwareAccounts = viewModel::onSkipRemainingHardwareAccounts,
        accountsLeft = state.hardwareAccountsLeftToImport,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        addLedgerName = state.addLedgerName,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        onSkipLedgerName = viewModel::onSkipLedgerName,
        ledgerDevices = state.ledgerDevices,
        hasP2pLinks = state.hasP2pLinks,
        onAddP2PLink = onAddP2PLink
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
    onHardwareImport: () -> Unit,
    currentPage: ImportPage,
    onToggleSelectAll: () -> Unit,
    qrChunkInfo: ChunkInfo?,
    onMnemonicAlreadyImported: () -> Unit,
    isDeviceSecure: Boolean,
    onSkipRemainingHardwareAccounts: () -> Unit,
    accountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    addLedgerName: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    ledgerDevices: ImmutableMap<LedgerDeviceUiModel, Int>,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    var cameraVisible by remember {
        mutableStateOf(false)
    }
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler {
        if (currentPage == ImportPage.ImportComplete || currentPage == ImportPage.ScanQr) {
            onCloseScreen()
        } else {
            onBackClick()
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
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.import_legacy_wallet),
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
                            onHardwareImport = onHardwareImport,
                            accountsLeft = accountsLeft,
                            onSkipRemainingHardwareAccounts = onSkipRemainingHardwareAccounts,
                            waitingForLedgerResponse = waitingForLedgerResponse,
                            addLedgerName = addLedgerName,
                            onConfirmLedgerName = onConfirmLedgerName,
                            onSkipLedgerName = onSkipLedgerName,
                            ledgerDevices = ledgerDevices,
                            hasP2pLinks = hasP2pLinks,
                            onAddP2PLink = onAddP2PLink
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
                        text = stringResource(id = R.string.scanned_x_out_of_y, chunkInfo.scanned, chunkInfo.total),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }
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
            text = stringResource(R.string.import_olympia_accounts),
            onClick = onImportAccounts,
            enabled = importButtonEnabled,
            throttleClicks = true
        )
    }
}

@Composable
private fun HardwareImportScreen(
    modifier: Modifier = Modifier,
    onHardwareImport: () -> Unit,
    accountsLeft: Int,
    onSkipRemainingHardwareAccounts: () -> Unit,
    waitingForLedgerResponse: Boolean,
    addLedgerName: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    ledgerDevices: ImmutableMap<LedgerDeviceUiModel, Int>,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit
) {
    var ledgerNameValue by remember {
        mutableStateOf("")
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = pluralStringResource(id = R.plurals.accounts_left_to_import, accountsLeft, accountsLeft),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            if (!hasP2pLinks) {
                Text(
                    text = stringResource(id = com.babylon.wallet.android.R.string.found_no_radix_connect_connections),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAddP2PLink,
                    text = stringResource(id = com.babylon.wallet.android.R.string.add_new_p2p_link)
                )
            }
            if (addLedgerName) {
                Spacer(modifier = Modifier.weight(1f))
                RadixTextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = { ledgerNameValue = it },
                    value = ledgerNameValue,
                    leftLabel = stringResource(id = R.string.name_this_ledger),
                    hint = stringResource(id = R.string.ledger_hint),
                    optionalHint = stringResource(id = R.string.ledger_name_bottom_hint)
                )
                RadixPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.confirm_name),
                    onClick = {
                        onConfirmLedgerName(ledgerNameValue)
                        ledgerNameValue = ""
                    },
                    throttleClicks = true
                )
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    text = stringResource(R.string.skip),
                    onClick = onSkipLedgerName,
                    throttleClicks = true
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    items(ledgerDevices.entries.toList()) { entry ->
                        ImportedHardwareAccountsRow(
                            deviceName = entry.key.name ?: entry.key.model.name,
                            importedAccountsCount = entry.value,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    RadixTheme.colors.gray5,
                                    shape = RadixTheme.shapes.roundedRectMedium
                                )
                                .padding(RadixTheme.dimensions.paddingDefault)
                        )
                    }
                }
                AnimatedVisibility(visible = hasP2pLinks, enter = fadeIn(), exit = fadeOut()) {
                    RadixPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.send_add_ledger_request),
                        onClick = onHardwareImport,
                        enabled = !waitingForLedgerResponse,
                        throttleClicks = true
                    )
                }
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.skip_remaining_accounts),
                    onClick = onSkipRemainingHardwareAccounts,
                    enabled = !waitingForLedgerResponse,
                    throttleClicks = true
                )
            }
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun ImportedHardwareAccountsRow(deviceName: String, importedAccountsCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Text(
            text = deviceName,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = pluralStringResource(
                id = R.plurals.accounts_per_device,
                count = importedAccountsCount,
                importedAccountsCount
            ),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
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
                    text = pluralStringResource(id = R.plurals.imported_x_accounts, migratedAccounts.size, migratedAccounts.size),
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
            text = stringResource(R.string.continue_button_title),
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
            leftLabel = stringResource(id = R.string.seed_phrase),
            hint = stringResource(id = R.string.seed_phrase),
        )
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = onPassphraseChanged,
            value = bip39Passphrase,
            leftLabel = stringResource(id = R.string.bip_39_passphrase),
            hint = stringResource(id = R.string.passphrase),
        )
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.import_label),
            onClick = onImportSoftwareAccounts,
            enabled = importSoftwareAccountsEnabled,
            throttleClicks = true
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.already_imported),
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
            onHardwareImport = {},
            currentPage = ImportPage.ScanQr,
            onToggleSelectAll = {},
            qrChunkInfo = null,
            onMnemonicAlreadyImported = {},
            isDeviceSecure = true,
            onSkipRemainingHardwareAccounts = {},
            accountsLeft = 5,
            waitingForLedgerResponse = false,
            addLedgerName = false,
            onConfirmLedgerName = {},
            onSkipLedgerName = {},
            ledgerDevices = persistentMapOf(),
            hasP2pLinks = true,
            onAddP2PLink = {}
        )
    }
}
