package com.babylon.wallet.android.presentation.settings

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.Orange1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.backup.backupMessage
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.sheets.SheetHeader
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onProfileDeleted: () -> Unit,
    onConnectionPasswordDecoded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        onBackClick = onBackClick,
        onDeleteWalletClick = viewModel::onDeleteWalletClick,
        appSettings = state.settings,
        onSettingClick = onSettingClick,
        onConnectionPasswordDecoded = viewModel::onConnectionPasswordDecoded,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SettingsEvent.ProfileDeleted -> onProfileDeleted()
                is SettingsEvent.ConnectionPasswordDecoded -> {
                    onConnectionPasswordDecoded(event.connectionPassword)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@Composable
private fun SettingsContent(
    onBackClick: () -> Unit,
    onDeleteWalletClick: () -> Unit,
    appSettings: ImmutableList<SettingsItem.TopLevelSettings>,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onConnectionPasswordDecoded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        wrapContent = true,
        enableImePadding = true,
        sheetContent = {
            if (cameraPermissionState.status.isGranted) {
                ScanQRSheet(
                    modifier = Modifier
                        .background(color = RadixTheme.colors.white),
                    onAddressDecoded = {
                        onConnectionPasswordDecoded(it)
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    onCloseClick = {
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.Start
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1,
                titleIcon = {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_settings),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "settings gear"
                    )
                }
            )
            Divider(color = RadixTheme.colors.gray4)
            LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                appSettings.forEach { settingsItem ->
                    when (settingsItem) {
                        SettingsItem.TopLevelSettings.Connection -> {
                            item {
                                ConnectionSettingItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5)
                                        .padding(RadixTheme.dimensions.paddingDefault),
                                    onSettingClick = {
                                        scope.launch {
                                            bottomSheetState.show()
                                        }
                                    },
                                    settingsItem = settingsItem
                                )
                            }
                        }
                        SettingsItem.TopLevelSettings.DeleteAll -> {
                            settingsItem.descriptionRes().let {
                                item {
                                    RadixSecondaryButton(
                                        modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                                        text = stringResource(id = it),
                                        onClick = onDeleteWalletClick,
                                        contentColor = RadixTheme.colors.red1
                                    )
                                }
                            }
                        }
                        else -> {
                            item {
                                if (settingsItem is SettingsItem.TopLevelSettings.Backups) {
                                    BackupSettingsItem(
                                        backupSettingsItem = settingsItem,
                                        onClick = {
                                            onSettingClick(settingsItem)
                                        }
                                    )
                                } else {
                                    DefaultSettingsItem(
                                        settingsItem = settingsItem,
                                        onClick = {
                                            onSettingClick(settingsItem)
                                        }
                                    )
                                }
                            }
                            item {
                                Divider(color = RadixTheme.colors.gray5)
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(
                            R.string.settings_appVersion,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE.toString()
                        ),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }
            }
        }
    }
}

@Composable
private fun DefaultSettingsItem(
    settingsItem: SettingsItem.TopLevelSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        settingsItem.getIcon()?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }
        Text(
            text = stringResource(id = settingsItem.descriptionRes()),
            style = RadixTheme.typography.body2Header,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun BackupSettingsItem(
    backupSettingsItem: SettingsItem.TopLevelSettings.Backups,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        backupSettingsItem.getIcon()?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = stringResource(id = backupSettingsItem.descriptionRes()),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = backupMessage(state = backupSettingsItem.backupState),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )

                if (backupSettingsItem.backupState.isWarningVisible) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error),
                        contentDescription = null,
                        tint = Orange1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun ConnectionSettingItem(
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    settingsItem: SettingsItem.TopLevelSettings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Image(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_connector),
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.settings_linkToConnectorHeader_title),
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.settings_linkToConnectorHeader_subtitle),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center
        )
        RadixSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(R.string.settings_linkToConnectorHeader_linkToConnector),
            onClick = {
                onSettingClick(settingsItem)
            },
            containerColor = RadixTheme.colors.gray3,
            contentColor = RadixTheme.colors.gray1,
            icon = {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                    ),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
fun ScanQRSheet(
    modifier: Modifier = Modifier,
    onAddressDecoded: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            title = "",
            onLeadingActionClicked = onCloseClick
        )

        Text(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.linkedConnectors_linkNewConnector),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )

        Text(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.linkedConnectors_newConnection_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        CameraPreview(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .imePadding()
                .clip(RadixTheme.shapes.roundedRectMedium)
        ) {
            onAddressDecoded(it)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            onBackClick = {},
            onDeleteWalletClick = {},
            appSettings = persistentListOf(
                SettingsItem.TopLevelSettings.Connection,
                SettingsItem.TopLevelSettings.LinkedConnector,
                SettingsItem.TopLevelSettings.Gateways,
                SettingsItem.TopLevelSettings.AuthorizedDapps,
                SettingsItem.TopLevelSettings.Personas,
                SettingsItem.TopLevelSettings.DeleteAll
            ),
            onSettingClick = {},
            onConnectionPasswordDecoded = {}
        )
    }
}
