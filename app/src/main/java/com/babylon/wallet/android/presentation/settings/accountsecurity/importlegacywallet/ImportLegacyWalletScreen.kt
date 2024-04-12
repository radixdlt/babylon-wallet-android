@file:Suppress("CyclomaticComplexMethod", "TooManyFunctions")
@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class
)

package com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseWord
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet.ImportLegacyWalletUiState.Page
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.AddLinkConnectorUiState
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.AddLedgerDeviceUiState
import com.babylon.wallet.android.presentation.ui.MockUiProvider.accountItemUiModelsList
import com.babylon.wallet.android.presentation.ui.MockUiProvider.olympiaAccountsList
import com.babylon.wallet.android.presentation.ui.MockUiProvider.seedPhraseWords
import com.babylon.wallet.android.presentation.ui.composables.AccountCardWithStack
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RedWarningText
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseSuggestions
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.string
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
fun ImportLegacyWalletScreen(
    viewModel: ImportLegacyWalletViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onCloseScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    ImportLegacyWalletContent(
        modifier = modifier,
        onBackClick = viewModel::onBackClick,
        onQrCodeScanned = viewModel::onQrCodeScanned,
        pages = state.pages,
        oneOffEvent = viewModel.oneOffEvent,
        olympiaAccountsToImport = state.olympiaAccountsToImport,
        onImportAccounts = {
            viewModel.onImportAccounts {
                context.biometricAuthenticateSuspend()
            }
        },
        onCloseScreen = onCloseScreen,
        importButtonEnabled = state.importButtonEnabled,
        onWordChanged = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onValidateSoftwareAccounts = {
            viewModel.onValidateSoftwareAccounts {
                context.biometricAuthenticateSuspend()
            }
        },
        uiMessage = state.uiMessage,
        onMessageShown = viewModel::onMessageShown,
        migratedAccounts = state.migratedAccounts,
        onContinue = onCloseScreen,
        currentPage = state.currentPage,
        qrChunkInfo = state.qrChunkInfo,
        hardwareAccountsLeft = state.hardwareAccountsLeftToImport,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        verifiedLedgerDevices = state.verifiedLedgerDevices,
        addLedgerSheetState = state.addLedgerSheetState,
        onContinueWithLedgerClick = viewModel::onContinueWithLedgerClick,
        deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
        shouldShowAddLinkConnectorScreen = state.shouldShowAddLinkConnectorScreen,
        addLinkConnectorState = addLinkConnectorState,
        onLinkConnectorQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
        onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
        onNewConnectorContinueClick = {
            addLinkConnectorViewModel.onContinueClick()
            viewModel.onNewConnectorAdded()
        },
        onNewConnectorCloseClick = {
            addLinkConnectorViewModel.onCloseClick()
            viewModel.onNewConnectorCloseClick()
        },
        shouldShowAddLedgerDeviceScreen = state.shouldShowAddLedgerDeviceScreen,
        onCloseSettings = viewModel::onCloseSettings,
        onWordSelected = viewModel::onWordSelected,
        importAllAccounts = viewModel::importAllAccounts,
        onInvalidConnectionPasswordShown = addLinkConnectorViewModel::onInvalidConnectionPasswordShown,
        seedPhraseInputState = state.seedPhraseInputState
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ImportLegacyWalletContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    pages: ImmutableList<Page>,
    oneOffEvent: Flow<OlympiaImportEvent>,
    olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails>,
    onImportAccounts: () -> Unit,
    onCloseScreen: () -> Unit,
    importButtonEnabled: Boolean,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onValidateSoftwareAccounts: () -> Unit,
    uiMessage: UiMessage?,
    onMessageShown: () -> Unit,
    migratedAccounts: ImmutableList<AccountItemUiModel>,
    onContinue: () -> Unit,
    currentPage: Page,
    qrChunkInfo: ChunkInfo?,
    hardwareAccountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    verifiedLedgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    addLedgerSheetState: AddLedgerDeviceUiState.ShowContent,
    onContinueWithLedgerClick: () -> Unit,
    deviceModel: String?,
    shouldShowAddLinkConnectorScreen: Boolean,
    addLinkConnectorState: AddLinkConnectorUiState,
    onLinkConnectorQrCodeScanned: (String) -> Unit,
    onConnectorDisplayNameChanged: (String) -> Unit,
    onNewConnectorContinueClick: () -> Unit,
    onNewConnectorCloseClick: () -> Unit,
    shouldShowAddLedgerDeviceScreen: Boolean,
    onCloseSettings: () -> Unit,
    onWordSelected: (Int, String) -> Unit,
    importAllAccounts: () -> Unit,
    onInvalidConnectionPasswordShown: () -> Unit,
    seedPhraseInputState: SeedPhraseInputDelegate.State
) {
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val pagerState = rememberPagerState(0) { pages.size }
    val scope = rememberCoroutineScope()
    var cameraVisible by remember {
        mutableStateOf(false)
    }
    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }
    val context = LocalContext.current
    BackHandler {
        when (currentPage) {
            Page.ImportComplete, Page.ScanQr -> {
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

                OlympiaImportEvent.BiometricPromptBeforeFinalImport -> {
                    context.biometricAuthenticate { result ->
                        if (result == BiometricAuthenticationResult.Succeeded) {
                            importAllAccounts()
                        }
                    }
                }

                OlympiaImportEvent.MoveFocusToNextWord -> {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            }
        }
    }
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    Box(modifier = modifier) {
        Scaffold(
            topBar = {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.empty),
                    onBackClick = if (currentPage == Page.ImportComplete) onCloseScreen else onBackClick,
                    backIconType = if (currentPage == Page.ImportComplete) BackIconType.Close else BackIconType.Back,
                    windowInsets = WindowInsets.statusBars
                )
            },
            snackbarHost = {
                RadixSnackbarHost(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    hostState = snackBarHostState
                )
            },
            containerColor = RadixTheme.colors.defaultBackground,
            bottomBar = {
                if (seedPhraseSuggestionsVisible(wordAutocompleteCandidates = seedPhraseInputState.wordAutocompleteCandidates)) {
                    SeedPhraseSuggestions(
                        wordAutocompleteCandidates = seedPhraseInputState.wordAutocompleteCandidates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .height(56.dp)
                            .padding(RadixTheme.dimensions.paddingSmall),
                        onCandidateClick = { candidate ->
                            focusedWordIndex?.let {
                                onWordSelected(it, candidate)
                                focusedWordIndex = null
                            }
                        }
                    )
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (pages[page]) {
                    Page.ScanQr -> {
                        ScanQrPage(
                            cameraPermissionGranted = cameraPermissionState.status.isGranted,
                            onQrCodeScanned = onQrCodeScanned,
                            isVisible = cameraVisible,
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            qrChunkInfo = qrChunkInfo
                        )
                    }

                    Page.AccountsToImportList -> {
                        AccountsToImportListPage(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            olympiaAccountsToImport = olympiaAccountsToImport,
                            onImportAccounts = onImportAccounts,
                            importButtonEnabled = importButtonEnabled,
                        )
                    }

                    Page.MnemonicInput -> {
                        VerifyWithYourSeedPhrasePage(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            seedPhraseWords = seedPhraseInputState.seedPhraseWords,
                            bip39Passphrase = seedPhraseInputState.bip39Passphrase,
                            onWordChanged = onWordChanged,
                            onPassphraseChanged = onPassphraseChanged,
                            onImportSoftwareAccounts = onValidateSoftwareAccounts,
                            onFocusedWordIndexChanged = {
                                focusedWordIndex = it
                            },
                            seedPhraseInputState = seedPhraseInputState
                        )
                    }

                    Page.HardwareAccounts -> {
                        VerifyWithLedgerDevicePage(
                            Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            hardwareAccountsLeft = hardwareAccountsLeft,
                            waitingForLedgerResponse = waitingForLedgerResponse,
                            verifiedLedgerDevices = verifiedLedgerDevices,
                            onContinueWithLedgerClick = onContinueWithLedgerClick
                        )
                    }

                    Page.ImportComplete -> {
                        ImportCompletePage(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            migratedAccounts = migratedAccounts,
                            onContinue = onContinue
                        )
                    }
                }
            }
        }
        if (shouldShowAddLinkConnectorScreen) {
            AddLinkConnectorScreen(
                modifier = Modifier.fillMaxSize(),
                showContent = addLinkConnectorState.showContent,
                onQrCodeScanned = onLinkConnectorQrCodeScanned,
                onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
                connectorDisplayName = addLinkConnectorState.connectorDisplayName,
                isNewConnectorContinueButtonEnabled = addLinkConnectorState.isContinueButtonEnabled,
                onNewConnectorContinueClick = onNewConnectorContinueClick,
                onNewConnectorCloseClick = onNewConnectorCloseClick,
                invalidConnectionPassword = addLinkConnectorState.invalidConnectionPassword,
                onInvalidConnectionPasswordDismissed = onInvalidConnectionPasswordShown
            )
        }
        if (shouldShowAddLedgerDeviceScreen) {
            AddLedgerDeviceScreen(
                modifier = Modifier
                    .fillMaxSize(),
                showContent = addLedgerSheetState,
                deviceModel = deviceModel,
                onSendAddLedgerRequestClick = onContinueWithLedgerClick,
                onConfirmLedgerNameClick = {
                    onConfirmLedgerName(it)
                    onCloseSettings()
                },
                backIconType = BackIconType.Back,
                onClose = onCloseSettings,
                waitingForLedgerResponse = waitingForLedgerResponse,
                onBackClick = onCloseSettings,
                isAddingLedgerDeviceInProgress = false

            )
        }
    }
}

@Composable
private fun seedPhraseSuggestionsVisible(wordAutocompleteCandidates: ImmutableList<String>): Boolean {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val kbVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    return wordAutocompleteCandidates.isNotEmpty() && kbVisible
}

@Composable
private fun CameraVisibilityEffect(
    pagerState: PagerState,
    pages: ImmutableList<Page>,
    onCameraVisibilityChanged: (Boolean) -> Unit
) {
    LaunchedEffect(pagerState, pages) {
        snapshotFlow {
            pagerState.currentPage == pages.indexOf(Page.ScanQr)
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
    if (cameraPermissionGranted) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = RadixTheme.dimensions.paddingXXLarge,
                    end = RadixTheme.dimensions.paddingXXLarge,
                    bottom = RadixTheme.dimensions.paddingDefault
                ),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSemiLarge),
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
                text = stringResource(id = R.string.importOlympiaAccounts_scanQR_instructions),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            CameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RadixTheme.shapes.roundedRectMedium),
                disableBackHandler = false,
                isVisible = isVisible,
                onQrCodeDetected = onQrCodeScanned
            )
        }
    }
}

@Composable
private fun AccountsToImportListPage(
    modifier: Modifier = Modifier,
    olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails>,
    onImportAccounts: () -> Unit,
    importButtonEnabled: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingMedium),
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
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
                    text = stringResource(id = R.string.importOlympiaAccounts_accountsToImport_subtitle),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
            items(olympiaAccountsToImport) { item ->
                val gradientColor = getAccountGradientColorsFor(item.appearanceId)
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
                olympiaAccountsToImport.size
            ),
            onClick = onImportAccounts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            enabled = importButtonEnabled
        )
    }
}

@Composable
private fun VerifyWithLedgerDevicePage(
    modifier: Modifier = Modifier,
    hardwareAccountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    verifiedLedgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    onContinueWithLedgerClick: () -> Unit
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
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .padding(bottom = RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.importOlympiaLedgerAccounts_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.importOlympiaLedgerAccounts_subtitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AccountsLeftText(hardwareAccountsLeft)
                if (verifiedLedgerDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    Text(
                        text = stringResource(id = R.string.importOlympiaLedgerAccounts_listHeading),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(items = verifiedLedgerDevices, key = { item ->
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
                    }
                }
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.importOlympiaLedgerAccounts_instruction),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            RadixPrimaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger),
                onClick = onContinueWithLedgerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault)
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
        text = stringResource(id = R.string.importOlympiaLedgerAccounts_accountCount, accountsLeft),
        style = RadixTheme.typography.body1Header,
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
            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.importOlympiaAccounts_completion_title),
            maxLines = 1,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.importOlympiaAccounts_completion_subtitleMultiple),
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
            itemsIndexed(migratedAccounts) { index, item ->
                val gradientColor = getAccountGradientColorsFor(item.appearanceID)
                if (index == migratedAccounts.size - 1) {
                    AccountCardWithStack(
                        Modifier.fillMaxWidth(0.8f),
                        item.appearanceID,
                        item.displayName.orEmpty(),
                        item.address.string
                    )
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
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                Text(
                    modifier = Modifier.padding(
                        start = RadixTheme.dimensions.paddingLarge,
                        end = RadixTheme.dimensions.paddingLarge,
                        top = RadixTheme.dimensions.paddingXXLarge
                    ),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            throttleClicks = true
        )
    }
}

@Composable
private fun VerifyWithYourSeedPhrasePage(
    modifier: Modifier = Modifier,
    seedPhraseWords: ImmutableList<SeedPhraseWord>,
    bip39Passphrase: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onImportSoftwareAccounts: () -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
    seedPhraseInputState: SeedPhraseInputDelegate.State
) {
    var showOlympiaSeedPhrasePrompt by remember { mutableStateOf(false) }
    if (showOlympiaSeedPhrasePrompt) {
        BasicPromptAlertDialog(
            finish = { confirmed ->
                if (confirmed) {
                    onImportSoftwareAccounts()
                }
                showOlympiaSeedPhrasePrompt = false
            },
            message = {
                Text(
                    text = stringResource(id = R.string.importOlympiaAccounts_verifySeedPhrase_keepSeedPhrasePrompt),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.importOlympiaAccounts_verifySeedPhrase_keepSeedPhrasePromptConfirmation),
            dismissText = null
        )
    }
    SecureScreen()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = RadixTheme.dimensions.paddingLarge,
                end = RadixTheme.dimensions.paddingLarge
            )
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSemiLarge)
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
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
        SeedPhraseInputForm(
            seedPhraseWords = seedPhraseWords,
            onWordChanged = onWordChanged,
            onPassphraseChanged = onPassphraseChanged,
            bip39Passphrase = bip39Passphrase,
            modifier = Modifier.fillMaxWidth(),
            onFocusedWordIndexChanged = onFocusedWordIndexChanged
        )
        if (seedPhraseInputState.seedPhraseInputValid && seedPhraseInputState.seedPhraseBIP39Valid.not()) {
            RedWarningText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                text = AnnotatedString(stringResource(R.string.importMnemonic_checksumFailure))
            )
        }
        RadixPrimaryButton(
            text = stringResource(R.string.importOlympiaAccounts_importLabel),
            onClick = {
                showOlympiaSeedPhrasePrompt = true
            },
            modifier = Modifier.fillMaxWidth(),
            throttleClicks = true,
            enabled = seedPhraseInputState.seedPhraseInputValid && seedPhraseInputState.seedPhraseBIP39Valid
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun AccountListPagePreview() {
    RadixWalletTheme {
        AccountsToImportListPage(
            modifier = Modifier,
            olympiaAccountsToImport = olympiaAccountsList.toPersistentList(),
            onImportAccounts = {},
            importButtonEnabled = true
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun InputSeedPhrasePagePreview() {
    RadixWalletTheme {
        VerifyWithYourSeedPhrasePage(
            seedPhraseWords = seedPhraseWords,
            bip39Passphrase = "test",
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onImportSoftwareAccounts = {},
            onFocusedWordIndexChanged = {},
            seedPhraseInputState = SeedPhraseInputDelegate.State()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoVerifiedLedgersPreview() {
    RadixWalletTheme {
        VerifyWithLedgerDevicePage(
            modifier = Modifier,
            hardwareAccountsLeft = 5,
            waitingForLedgerResponse = false,
            verifiedLedgerDevices = persistentListOf(),
            onContinueWithLedgerClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportWithVerifiedLedgersPreview() {
    RadixWalletTheme {
        VerifyWithLedgerDevicePage(
            modifier = Modifier,
            hardwareAccountsLeft = 3,
            waitingForLedgerResponse = false,
            verifiedLedgerDevices = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onContinueWithLedgerClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoAccountsLeftPreview() {
    RadixWalletTheme {
        VerifyWithLedgerDevicePage(
            modifier = Modifier,
            hardwareAccountsLeft = 0,
            waitingForLedgerResponse = true,
            verifiedLedgerDevices = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onContinueWithLedgerClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ImportCompletePagePreview() {
    RadixWalletTheme {
        ImportCompletePage(
            modifier = Modifier,
            migratedAccounts = accountItemUiModelsList,
            onContinue = {}
        )
    }
}
