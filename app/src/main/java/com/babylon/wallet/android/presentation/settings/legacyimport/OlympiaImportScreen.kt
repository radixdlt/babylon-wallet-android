@file:Suppress("CyclomaticComplexMethod", "TooManyFunctions")
@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)

package com.babylon.wallet.android.presentation.settings.legacyimport

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.MockUiProvider.seedPhraseWords
import com.babylon.wallet.android.presentation.ui.composables.AccountCardWithStack
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerContent
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
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
        seedPhraseWords = state.seedPhraseWords,
        bip39Passphrase = state.bip39Passphrase,
        onWordChanged = viewModel::onWordChanged,
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
        deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
        totalHardwareAccounts = state.totalHardwareAccounts,
        wordAutocompleteCandidates = state.wordAutocompleteCandidates
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
    seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord>,
    bip39Passphrase: String,
    onWordChanged: (Int, String) -> Unit,
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
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    addLedgerSheetState: AddLedgerSheetState,
    onSendAddLedgerRequest: () -> Unit,
    deviceModel: String?,
    totalHardwareAccounts: Int,
    wordAutocompleteCandidates: ImmutableList<String>
) {
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    var cameraVisible by remember {
        mutableStateOf(false)
    }
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
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
            currentPage == ImportPage.ImportComplete || currentPage == ImportPage.ScanQr -> {
                onCloseScreen()
            }
            else -> {
                onBackClick()
            }
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

                OlympiaImportEvent.MoveFocusToNextWord -> {
                    focusManager.moveFocus(FocusDirection.Next)
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
        DefaultModalSheetLayout(modifier = Modifier.fillMaxSize(), sheetState = bottomSheetState, sheetContent = {
            AddLedgerContent(
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
                upIcon = {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "navigate back"
                    )
                },
                onClose = { closeSheetCallback() },
                waitingForLedgerResponse = waitingForLedgerResponse,
                onAddP2PLink = onAddP2PLink
            )
        }) {
            Column(modifier = Modifier.fillMaxSize()) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.empty),
                    onBackClick = if (currentPage == ImportPage.ImportComplete) onCloseScreen else onBackClick,
                    contentColor = RadixTheme.colors.gray1,
                    backIconType = if (currentPage == ImportPage.ImportComplete) BackIconType.Close else BackIconType.Back
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
                            InputSeedPhrasePage(
                                modifier = Modifier.fillMaxSize(),
                                seedPhraseWords = seedPhraseWords,
                                bip39Passphrase = bip39Passphrase,
                                onWordChanged = onWordChanged,
                                onPassphraseChanged = onPassphraseChanged,
                                importSoftwareAccountsEnabled = importSoftwareAccountsEnabled,
                                onImportSoftwareAccounts = onImportSoftwareAccounts,
                                onMnemonicAlreadyImported = onMnemonicAlreadyImported,
                                wordAutocompleteCandidates = wordAutocompleteCandidates
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
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.importOlympiaAccounts_scanQR_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                qrChunkInfo?.let { chunkInfo ->
                    Text(
                        text = stringResource(
                            id = R.string.importOlympiaAccounts_scanQR_scannedLabel,
                            chunkInfo.scanned,
                            chunkInfo.total
                        ),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }
                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.importOlympiaAccounts_scanQR_instructions),
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
            text = stringResource(id = R.string.importOlympiaAccounts_accountsToImport_title),
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
                    text = stringResource(id = R.string.importOlympiaAccounts_accountsToImport_subtitle),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
            if (olympiaAccounts.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                        text = "No accounts found to import.", // TODO will be removed
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
                    accountName = item.accountName,
                    accountType = item.type,
                    address = item.address,
                    newAddress = item.newBabylonAddress
                )
            }
        }
        RadixPrimaryButton(
            text = stringResource(
                R.string.importOlympiaAccounts_accountsToImport_buttonManyAccounts,
                olympiaAccounts.size
            ),
            onClick = onImportAccounts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
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
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
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
                    text = stringResource(id = R.string.importOlympiaLedgerAccounts_subtitle, totalHardwareAccounts),
                    style = RadixTheme.typography.header,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = stringResource(id = R.string.importOlympiaLedgerAccounts_listHeading),
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
                        text = stringResource(id = R.string.importOlympiaLedgerAccounts_knownLedgersNone),
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
                        items(items = ledgerFactorSources, key = { item ->
                            item.id.body.value
                        }, itemContent = { item ->
                                LedgerListItem(
                                    ledgerFactorSource = item,
                                    modifier = Modifier
                                        .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                                        .padding(RadixTheme.dimensions.paddingLarge),
                                )
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                            })
                        item {
                            AccountsLeftText(accountsLeft)
                        }
                    }
                }
            }
            RadixPrimaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger),
                onClick = onUseLedger,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
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
        text = stringResource(id = R.string.importOlympiaLedgerAccounts_otherDeviceAccounts, accountsLeft),
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
            text = stringResource(id = R.string.importOlympiaAccounts_completion_title),
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
                    text = stringResource(id = R.string.importOlympiaAccounts_completion_subtitleMultiple),
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
                    text = stringResource(id = R.string.importOlympiaAccounts_completion_explanation),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
        }
        RadixPrimaryButton(
            text = stringResource(R.string.importOlympiaAccounts_completion_accountListButtonTitle),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            throttleClicks = true
        )
    }
}

@Composable
private fun InputSeedPhrasePage(
    modifier: Modifier = Modifier,
    seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord>,
    bip39Passphrase: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    importSoftwareAccountsEnabled: Boolean,
    onImportSoftwareAccounts: () -> Unit,
    onMnemonicAlreadyImported: () -> Unit,
    wordAutocompleteCandidates: ImmutableList<String>
) {
    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val kbVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val isSeedPhraseSuggestionsVisible = wordAutocompleteCandidates.isNotEmpty() && kbVisible
    val stripHeight by animateDpAsState(
        targetValue = if (isSeedPhraseSuggestionsVisible) {
            candidatesStripHeight
        } else {
            0.dp
        }
    )
    SecureScreen()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(bottom = stripHeight)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.importOlympiaAccounts_verifySeedPhrase_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.importOlympiaAccounts_verifySeedPhrase_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            InfoLink(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.importOlympiaAccounts_verifySeedPhrase_warning),
                contentColor = RadixTheme.colors.orange1,
                iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            )
            SeedPhraseInputForm(
                seedPhraseWords = seedPhraseWords,
                onWordChanged = onWordChanged,
                onPassphraseChanged = onPassphraseChanged,
                bip39Passphrase = bip39Passphrase,
                modifier = Modifier.fillMaxWidth(),
                onFocusedWordIndexChanged = {
                    focusedWordIndex = it
                }
            )
            RadixPrimaryButton(
                text = stringResource(R.string.importOlympiaAccounts_importLabel),
                onClick = onImportSoftwareAccounts,
                modifier = Modifier.fillMaxWidth(),
                enabled = importSoftwareAccountsEnabled,
                throttleClicks = true
            )
            if (importSoftwareAccountsEnabled) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.importOlympiaAccounts_alreadyImported),
                    onClick = onMnemonicAlreadyImported,
                    throttleClicks = true
                )
            }
        }
        if (isSeedPhraseSuggestionsVisible) {
            SeedPhraseSuggestions(
                wordAutocompleteCandidates = wordAutocompleteCandidates,
                modifier = Modifier
                    .imePadding()
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(candidatesStripHeight)
                    .padding(RadixTheme.dimensions.paddingSmall),
                onCandidateClick = { candidate ->
                    focusedWordIndex?.let {
                        onWordChanged(it, candidate)
                        focusedWordIndex = null
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoVerifiedLedgersPreview() {
    RadixWalletTheme {
        HardwareImportScreen(
            modifier = Modifier,
            totalHardwareAccounts = 5,
            accountsLeft = 5,
            waitingForLedgerResponse = false,
            ledgerFactorSources = persistentListOf(),
            onUseLedger = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportWithVerifiedLedgersPreview() {
    RadixWalletTheme {
        HardwareImportScreen(
            modifier = Modifier,
            totalHardwareAccounts = 5,
            accountsLeft = 3,
            waitingForLedgerResponse = false,
            ledgerFactorSources = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onUseLedger = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoAccountsLeftPreview() {
    RadixWalletTheme {
        HardwareImportScreen(
            modifier = Modifier,
            totalHardwareAccounts = 5,
            accountsLeft = 0,
            waitingForLedgerResponse = true,
            ledgerFactorSources = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onUseLedger = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InputSeedPhrasePagePreview() {
    RadixWalletTheme {
        InputSeedPhrasePage(
            seedPhraseWords = seedPhraseWords,
            bip39Passphrase = "test",
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            importSoftwareAccountsEnabled = false,
            onImportSoftwareAccounts = {},
            onMnemonicAlreadyImported = {},
            wordAutocompleteCandidates = persistentListOf()
        )
    }
}

private val candidatesStripHeight = 56.dp
