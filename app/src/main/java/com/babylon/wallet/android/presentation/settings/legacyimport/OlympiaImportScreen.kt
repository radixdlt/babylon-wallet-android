@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.settings.legacyimport

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Divider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.OlympiaAccountDetails
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun OlympiaImportScreen(
    viewModel: OlympiaImportViewModel,
    onCloseScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OlympiaImportContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = viewModel::onBackClick,
        onQrCodeScanned = viewModel::onQrCodeScanned,
        triggerCameraPermissionPrompt = state.triggerCameraPermissionPrompt,
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
        onMessageShown = viewModel::onMessageShown
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OlympiaImportContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    triggerCameraPermissionPrompt: Boolean,
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
    onMessageShown: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    var cameraVisible by remember {
        mutableStateOf(false)
    }
    BackHandler {
        onBackClick()
    }
    LaunchedEffect(Unit) {
        snapshotFlow { triggerCameraPermissionPrompt }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                cameraPermissionState.launchPermissionRequest()
            }
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
            }

        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.import_legacy_wallet),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
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
                        ScnQrPage(
                            cameraPermissionState = cameraPermissionState,
                            onQrCodeScanned = onQrCodeScanned,
                            isVisible = cameraVisible,
                            modifier = Modifier.fillMaxSize()
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
                            onImportSoftwareAccounts = onImportSoftwareAccounts
                        )
                    }
                    ImportPage.HardwareAccount -> {}
                    ImportPage.ImportComplete -> {}
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
@OptIn(ExperimentalPermissionsApi::class)
private fun ScnQrPage(
    cameraPermissionState: PermissionState,
    onQrCodeScanned: (String) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingLarge)
                    .clip(RadixTheme.shapes.roundedRectMedium),
                disableBack = false,
                isVisible = isVisible,
                onQrCodeDetected = onQrCodeScanned
            )
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
                LegacyAccountSelectionCard(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(gradientColor),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .clip(RadixTheme.shapes.roundedRectSmall)
                        .throttleClickable {
                            onAccountSelected(item)
                        },
                    accountName = item.data.accountName,
                    accountType = item.data.type.name,
                    checked = item.selected,
                    address = item.data.address,
                    path = item.data.derivationPath.path
                )
            }
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.import_olympia_accounts),
            onClick = onImportAccounts,
            enabled = importButtonEnabled
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
    onImportSoftwareAccounts: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.import_label),
            onClick = onImportSoftwareAccounts,
            enabled = importSoftwareAccountsEnabled
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
            triggerCameraPermissionPrompt = false,
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
            onMessageShown = {}
        )
    }
}