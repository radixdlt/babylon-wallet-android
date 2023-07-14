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
import com.babylon.wallet.android.presentation.common.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.MockUiProvider.olympiaAccountsList
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
        olympiaAccountsToImport = state.olympiaAccountsToImport,
        onImportAccounts = viewModel::onImportAccounts,
        onCloseScreen = onCloseScreen,
        importButtonEnabled = state.importButtonEnabled,
        seedPhraseWords = state.seedPhraseWords,
        bip39Passphrase = state.bip39Passphrase,
        onWordChanged = viewModel::onWordChanged,
        onPassphraseChanged = viewModel::onPassphraseChanged,
        onImportSoftwareAccounts = viewModel::onImportSoftwareAccounts,
        uiMessage = state.uiMessage,
        onMessageShown = viewModel::onMessageShown,
        migratedAccounts = state.migratedAccounts,
        onContinue = onCloseScreen,
        currentPage = state.currentPage,
        qrChunkInfo = state.qrChunkInfo,
        isDeviceSecure = state.isDeviceSecure,
        hardwareAccountsLeft = state.hardwareAccountsLeftToImport,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        onAddP2PLink = onAddP2PLink,
        verifiedLedgerDevices = state.verifiedLedgerDevices,
        addLedgerSheetState = state.addLedgerSheetState,
        onImportWithLedger = viewModel::onImportWithLedger,
        deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
        wordAutocompleteCandidates = state.wordAutocompleteCandidates,
        shouldShowBottomSheet = state.shouldShowBottomSheet,
        onHideBottomSheet = viewModel::onHideBottomSheet
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OlympiaImportContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    pages: ImmutableList<OlympiaImportUiState.Page>,
    oneOffEvent: Flow<OlympiaImportEvent>,
    olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails>,
    onImportAccounts: () -> Unit,
    onCloseScreen: () -> Unit,
    importButtonEnabled: Boolean,
    seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord>,
    bip39Passphrase: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onImportSoftwareAccounts: () -> Unit,
    uiMessage: UiMessage?,
    onMessageShown: () -> Unit,
    migratedAccounts: ImmutableList<AccountItemUiModel>,
    onContinue: () -> Unit,
    currentPage: OlympiaImportUiState.Page,
    qrChunkInfo: ChunkInfo?,
    isDeviceSecure: Boolean,
    hardwareAccountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    onAddP2PLink: () -> Unit,
    verifiedLedgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    addLedgerSheetState: AddLedgerSheetState,
    onImportWithLedger: () -> Unit,
    deviceModel: String?,
    wordAutocompleteCandidates: ImmutableList<String>,
    shouldShowBottomSheet: Boolean,
    onHideBottomSheet: () -> Unit
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
            onHideBottomSheet()
        }
    }
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler {
        when {
            bottomSheetState.isVisible -> {
                closeSheetCallback()
            }
            currentPage == OlympiaImportUiState.Page.ImportComplete || currentPage == OlympiaImportUiState.Page.ScanQr -> {
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
    LaunchedEffect(shouldShowBottomSheet) {
        if (shouldShowBottomSheet) {
            bottomSheetState.show()
        }
    }
    Box(modifier = modifier) {
        DefaultModalSheetLayout(modifier = Modifier.fillMaxSize(), sheetState = bottomSheetState, sheetContent = {
            AddLedgerContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingDefault),
                deviceModel = deviceModel,
                onSendAddLedgerRequest = onImportWithLedger,
                addLedgerSheetState = addLedgerSheetState,
                onConfirmLedgerName = {
                    onConfirmLedgerName(it)
                    closeSheetCallback()
                },
                backIconType = BackIconType.Back,
                onClose = { closeSheetCallback() },
                waitingForLedgerResponse = waitingForLedgerResponse,
                onAddP2PLink = onAddP2PLink
            )
        }) {
            Column(modifier = Modifier.fillMaxSize()) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.empty),
                    onBackClick = if (currentPage == OlympiaImportUiState.Page.ImportComplete) onCloseScreen else onBackClick,
                    contentColor = RadixTheme.colors.gray1,
                    backIconType = if (currentPage == OlympiaImportUiState.Page.ImportComplete) BackIconType.Close else BackIconType.Back
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
                        OlympiaImportUiState.Page.ScanQr -> {
                            ScanQrPage(
                                cameraPermissionGranted = cameraPermissionState.status.isGranted,
                                onQrCodeScanned = onQrCodeScanned,
                                isVisible = cameraVisible,
                                modifier = Modifier.fillMaxSize(),
                                qrChunkInfo = qrChunkInfo
                            )
                        }

                        OlympiaImportUiState.Page.AccountsToImportList -> {
                            AccountsToImportListPage(
                                modifier = Modifier.fillMaxSize(),
                                olympiaAccountsToImport = olympiaAccountsToImport,
                                onImportAccounts = onImportAccounts,
                                importButtonEnabled = importButtonEnabled,
                            )
                        }

                        OlympiaImportUiState.Page.MnemonicInput -> {
                            MnemonicInputPage(
                                modifier = Modifier.fillMaxSize(),
                                seedPhraseWords = seedPhraseWords,
                                bip39Passphrase = bip39Passphrase,
                                onWordChanged = onWordChanged,
                                onPassphraseChanged = onPassphraseChanged,
                                onImportSoftwareAccounts = onImportSoftwareAccounts,
                                wordAutocompleteCandidates = wordAutocompleteCandidates
                            )
                        }

                        OlympiaImportUiState.Page.HardwareAccounts -> {
                            LedgerAccountImportPage(
                                Modifier.fillMaxSize(),
                                hardwareAccountsLeft = hardwareAccountsLeft,
                                waitingForLedgerResponse = waitingForLedgerResponse,
                                verifiedLedgerDevices = verifiedLedgerDevices
                            ) {
                                onImportWithLedger()
                            }
                        }

                        OlympiaImportUiState.Page.ImportComplete -> {
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
    pages: ImmutableList<OlympiaImportUiState.Page>,
    onCameraVisibilityChanged: (Boolean) -> Unit
) {
    LaunchedEffect(pagerState, pages) {
        snapshotFlow {
            pagerState.currentPage == pages.indexOf(OlympiaImportUiState.Page.ScanQr)
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
private fun AccountsToImportListPage(
    modifier: Modifier = Modifier,
    olympiaAccountsToImport: ImmutableList<OlympiaAccountDetails>,
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
            items(olympiaAccountsToImport) { item ->
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
                olympiaAccountsToImport.size
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
private fun LedgerAccountImportPage(
    modifier: Modifier = Modifier,
    hardwareAccountsLeft: Int,
    waitingForLedgerResponse: Boolean,
    verifiedLedgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    onImportWithLedger: () -> Unit
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
                    .padding(top = RadixTheme.dimensions.paddingLarge),
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
                onClick = onImportWithLedger,
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
private fun MnemonicInputPage(
    modifier: Modifier = Modifier,
    seedPhraseWords: ImmutableList<SeedPhraseInputDelegate.SeedPhraseWord>,
    bip39Passphrase: String,
    onWordChanged: (Int, String) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onImportSoftwareAccounts: () -> Unit,
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
                .padding(RadixTheme.dimensions.paddingLarge),
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
                throttleClicks = true
            )
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

@Preview(showBackground = true)
@Composable
fun InputSeedPhrasePagePreview() {
    RadixWalletTheme {
        MnemonicInputPage(
            seedPhraseWords = seedPhraseWords,
            bip39Passphrase = "test",
            onWordChanged = { _, _ -> },
            onPassphraseChanged = {},
            onImportSoftwareAccounts = {},
            wordAutocompleteCandidates = persistentListOf()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoVerifiedLedgersPreview() {
    RadixWalletTheme {
        LedgerAccountImportPage(
            modifier = Modifier,
            hardwareAccountsLeft = 5,
            waitingForLedgerResponse = false,
            verifiedLedgerDevices = persistentListOf(),
            onImportWithLedger = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportWithVerifiedLedgersPreview() {
    RadixWalletTheme {
        LedgerAccountImportPage(
            modifier = Modifier,
            hardwareAccountsLeft = 3,
            waitingForLedgerResponse = false,
            verifiedLedgerDevices = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onImportWithLedger = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HardwareImportNoAccountsLeftPreview() {
    RadixWalletTheme {
        LedgerAccountImportPage(
            modifier = Modifier,
            hardwareAccountsLeft = 0,
            waitingForLedgerResponse = true,
            verifiedLedgerDevices = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onImportWithLedger = {}
        )
    }
}

private val candidatesStripHeight = 56.dp
