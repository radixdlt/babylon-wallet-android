@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.securitycenter.backup

import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.WarningButton
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.BackupState

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    onProfileDeleted: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackupScreenContent(
        modifier = modifier,
        state = state,
        onBackupCheckChanged = { isChecked ->
            viewModel.onBackupSettingChanged(isChecked)
        },
        onFileBackupClick = viewModel::onFileBackupClick,
        onFileBackupConfirm = viewModel::onFileBackupConfirm,
        onFileBackupDeny = viewModel::onFileBackupDeny,
        onEncryptPasswordTyped = viewModel::onEncryptPasswordTyped,
        onEncryptPasswordRevealToggle = viewModel::onEncryptPasswordRevealChange,
        onEncryptConfirmPasswordTyped = viewModel::onEncryptConfirmPasswordTyped,
        onEncryptPasswordConfirmRevealToggle = viewModel::onEncryptConfirmPasswordRevealChange,
        onEncryptSubmitClick = viewModel::onEncryptSubmitClick,
        onUiMessageShown = viewModel::onMessageShown,
        onDeleteWalletClick = viewModel::onDeleteWalletClick,
        onDeleteWalletConfirm = viewModel::onDeleteWalletConfirm,
        onDeleteWalletDeny = viewModel::onDeleteWalletDeny,
        onBackClick = viewModel::onBackClick,
        onDisconnectClick = {}
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType = "application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.onFileChosen(uri, deviceBiometricAuthenticationProvider = {
                context.biometricAuthenticateSuspend()
            })
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is BackupViewModel.Event.Dismiss -> onClose()
                is BackupViewModel.Event.ChooseExportFile -> filePickerLauncher.launch(it.fileName)
                is BackupViewModel.Event.ProfileDeleted -> onProfileDeleted()
                is BackupViewModel.Event.DeleteFile -> {
                    DocumentsContract.deleteDocument(context.contentResolver, it.file)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupScreenContent(
    modifier: Modifier = Modifier,
    state: BackupViewModel.State,
    onBackupCheckChanged: (Boolean) -> Unit,
    onFileBackupClick: () -> Unit,
    onFileBackupConfirm: (Boolean) -> Unit,
    onFileBackupDeny: () -> Unit,
    onEncryptPasswordTyped: (String) -> Unit,
    onEncryptPasswordRevealToggle: () -> Unit,
    onEncryptConfirmPasswordTyped: (String) -> Unit,
    onEncryptPasswordConfirmRevealToggle: () -> Unit,
    onEncryptSubmitClick: () -> Unit,
    onUiMessageShown: () -> Unit,
    onDeleteWalletClick: () -> Unit,
    onDeleteWalletConfirm: () -> Unit,
    onDeleteWalletDeny: () -> Unit,
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    if (state.isExportFileDialogVisible) {
        ExportWalletBackupFileDialog(
            onConfirm = onFileBackupConfirm,
            onDeny = onFileBackupDeny
        )
    }

    if (state.deleteWalletDialogVisible) {
        DeleteWalletDialog(
            onConfirm = onDeleteWalletConfirm,
            onDeny = onDeleteWalletDeny
        )
    }

    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    SyncSheetState(
        sheetState = modalBottomSheetState,
        isSheetVisible = state.isEncryptSheetVisible,
        onSheetClosed = {
            if (state.isEncryptSheetVisible) {
                onBackClick()
            }
        }
    )

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onUiMessageShown
    )

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.configurationBackup_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.configurationBackup_subtitle),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1Header
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            if (state.backupState.isWarningVisible) {
                BackupWarning()
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
            Column(
                modifier = Modifier
                    .shadow(6.dp)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium)
            ) {
                BackupStatusCard(state = state, onBackupCheckChanged = onBackupCheckChanged)
                if (state.isLoggedIn) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        color = RadixTheme.colors.gray5
                    )
                    LoggedInStatus(onDisconnectClick = onDisconnectClick)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectBottomMedium)
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingDefault),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    Icon(
                        painter = painterResource(id = DSR.ic_warning_error),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.configurationBackup_automatedBackupsWarning),
                        color = RadixTheme.colors.gray1,
                        style = RadixTheme.typography.body1Regular
                    )
                }
            }

            Text(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.configurationBackup_manualBackup_heading),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1Header
            )

            ManualBackupCard(onFileBackupClick = onFileBackupClick)
            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.profileBackup_deleteWallet_buttonTitle),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1HighImportance
            )

            Surface(
                color = RadixTheme.colors.defaultBackground,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingLarge
                        ),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    Text(
                        text = stringResource(id = R.string.androidProfileBackup_deleteWallet_subtitle)
                            .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.Bold)),
                        color = RadixTheme.colors.gray2,
                        style = RadixTheme.typography.body1HighImportance
                    )
                    WarningButton(
                        text = stringResource(R.string.androidProfileBackup_deleteWallet_confirmButton),
                        onClick = onDeleteWalletClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }

    if (state.isEncryptSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier.fillMaxSize(),
            sheetState = modalBottomSheetState,
            sheetContent = {
                if (state.encryptSheet is BackupViewModel.State.EncryptSheet.Open) {
                    EncryptSheet(
                        state = state.encryptSheet,
                        onPasswordTyped = onEncryptPasswordTyped,
                        onPasswordRevealToggle = onEncryptPasswordRevealToggle,
                        onPasswordConfirmTyped = onEncryptConfirmPasswordTyped,
                        onPasswordConfirmRevealToggle = onEncryptPasswordConfirmRevealToggle,
                        onSubmitClick = onEncryptSubmitClick,
                        onBackClick = onBackClick
                    )
                }
            },
            onDismissRequest = onBackClick
        )
    }
}

@Composable
private fun ManualBackupCard(
    modifier: Modifier = Modifier,
    onFileBackupClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(6.dp)
            .fillMaxWidth()
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium)
    ) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.configurationBackup_manualBackup_subtitle),
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular
        )
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.configurationBackup_manualBackup_exportButton),
            onClick = onFileBackupClick
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectBottomMedium)
                .padding(
                    RadixTheme.dimensions.paddingDefault
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_warning_error),
                contentDescription = null
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.configurationBackup_manualBackup_warning),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Regular
            )
        }
    }
}

@Composable
private fun BackupStatusCard(
    modifier: Modifier = Modifier,
    state: BackupViewModel.State,
    onBackupCheckChanged: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
    ) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        SwitchSettingsItem(
            titleRes = R.string.configurationBackup_backupsToggleGDrive,
            subtitleRes = R.string.configurationBackup_backupsUpdate,
            checked = state.isBackupEnabled,
            icon = {
                Icon(
                    painter = painterResource(id = DSR.ic_backup),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = null
                )
            },
            onCheckedChange = onBackupCheckChanged
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
            color = RadixTheme.colors.gray5
        )
        Text(
            text = stringResource(id = R.string.configurationBackup_automatedBackupsToggle),
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_accountsItem),
            subtitle = stringResource(id = R.string.configurationBackup_accountsSubtitle),
            backupState = state.backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_personasItem),
            subtitle = stringResource(id = R.string.configurationBackup_personasSubtitle),
            backupState = state.backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_securityFactorsItem),
            subtitle = stringResource(id = R.string.configurationBackup_securityFactorsSubtitle),
            backupState = state.backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_walletSettingsItem),
            subtitle = stringResource(id = R.string.configurationBackup_walletSettingsSubtitle),
            backupState = state.backupState
        )
    }
}

@Composable
private fun LoggedInStatus(modifier: Modifier = Modifier, onDisconnectClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectBottomMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.configurationBackup_loggedInAsHeading),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2Regular
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "Test User",
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Regular
            )
        }
        RadixTextButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.configurationBackup_disconnectButton),
            onClick = onDisconnectClick
        )
    }
}

@Composable
private fun BackupWarning(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.orange1.copy(alpha = 0.3f), RadixTheme.shapes.roundedRectMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.orange1.copy(alpha = 0.1f), RadixTheme.shapes.roundedRectMedium)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_warning_error),
                contentDescription = null,
                tint = RadixTheme.colors.orange1
            )
            Text(
                text = stringResource(id = R.string.configurationBackup_problem5WarningAndroid),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.orange1
            )
        }
    }
}

@Composable
private fun BackupStatusSection(title: String, subtitle: String, backupState: BackupState) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .clickable { expanded = !expanded }
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingSmall)
            .animateContentSize()
    ) {
        val statusColor = if (backupState.isWarningVisible) RadixTheme.colors.orange1 else RadixTheme.colors.green1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_check_circle),
                tint = statusColor,
                contentDescription = null
            )
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = statusColor,
                style = RadixTheme.typography.body2HighImportance
            )
            Icon(
                painter = painterResource(id = if (expanded) DSR.ic_arrow_up else DSR.ic_arrow_down),
                tint = RadixTheme.colors.gray1,
                contentDescription = null
            )
        }
        if (expanded) {
            Text(
                text = subtitle,
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2Regular
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportWalletBackupFileDialog(
    modifier: Modifier = Modifier,
    onConfirm: (Boolean) -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDeny,
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.defaultBackground,
        title = {
            Text(
                text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogTitle),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            ) {
                Text(
                    modifier = modifier
                        .clickable(role = Role.Button) { onConfirm(true) }
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingSmall,
                            vertical = RadixTheme.dimensions.paddingXSmall
                        ),
                    text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogConfirm),
                    color = RadixTheme.colors.red1
                )

                Text(
                    modifier = modifier
                        .clickable(role = Role.Button) { onConfirm(false) }
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingSmall,
                            vertical = RadixTheme.dimensions.paddingXSmall
                        ),
                    text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogDeny),
                    color = RadixTheme.colors.red1
                )
            }
        },
        dismissButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onDeny() }
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSmall,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    ),
                text = stringResource(id = R.string.common_cancel),
                color = RadixTheme.colors.blue2
            )
        }
    )
}

@Composable
private fun DeleteWalletDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDeny,
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.defaultBackground,
        text = {
            Text(
                text = stringResource(id = R.string.profileBackup_deleteWalletDialog_message),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        },
        confirmButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onConfirm() }
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSmall,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    ),
                text = stringResource(id = R.string.profileBackup_deleteWalletDialog_confirm),
                color = RadixTheme.colors.red1
            )
        },
        dismissButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onDeny() }
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSmall,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    ),
                text = stringResource(id = R.string.common_cancel),
                color = RadixTheme.colors.blue2
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSheetState(
    sheetState: SheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            onSheetClosed()
        }
    }
}

@Composable
private fun EncryptSheet(
    modifier: Modifier = Modifier,
    state: BackupViewModel.State.EncryptSheet.Open,
    onPasswordTyped: (String) -> Unit,
    onPasswordRevealToggle: () -> Unit,
    onPasswordConfirmTyped: (String) -> Unit,
    onPasswordConfirmRevealToggle: () -> Unit,
    onSubmitClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            RadixCenteredTopAppBar(title = "", onBackClick = onBackClick)
        },
        bottomBar = {
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.common_continue),
                onClick = onSubmitClick,
                enabled = state.isSubmitEnabled
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupTitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupSubtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                onValueChanged = onPasswordTyped,
                value = state.password,
                hint = stringResource(id = R.string.encryptProfileBackup_enterPasswordField_placeholder),
                trailingIcon = {
                    IconButton(onClick = onPasswordRevealToggle) {
                        Icon(
                            painter = painterResource(
                                id = if (state.isPasswordRevealed) {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_hide
                                } else {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_show
                                }
                            ),
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (state.isPasswordRevealed) VisualTransformation.None else PasswordVisualTransformation()
            )

            val focusManager = LocalFocusManager.current
            var isConfirmFocused by remember { mutableStateOf(false) }
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .onFocusChanged {
                        isConfirmFocused = it.isFocused
                    },
                onValueChanged = onPasswordConfirmTyped,
                value = state.confirm,
                hint = stringResource(id = R.string.encryptProfileBackup_confirmPasswordField_placeholder),
                error = if (!state.passwordsMatch && !isConfirmFocused && state.confirm.isNotBlank()) {
                    stringResource(id = R.string.encryptProfileBackup_confirmPasswordField_error)
                } else {
                    null
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordConfirmRevealToggle) {
                        Icon(
                            painter = painterResource(
                                id = if (state.isConfirmPasswordRevealed) {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_hide
                                } else {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_show
                                }
                            ),
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                }),
                visualTransformation = if (state.isConfirmPasswordRevealed) VisualTransformation.None else PasswordVisualTransformation()
            )
        }
    }
}

@Preview
@Composable
fun BackupScreenPreview() {
    RadixWalletTheme {
        BackupScreenContent(
            state = BackupViewModel.State(backupState = BackupState.Closed),
            onBackupCheckChanged = {},
            onFileBackupClick = {},
            onFileBackupConfirm = {},
            onFileBackupDeny = {},
            onEncryptPasswordTyped = {},
            onEncryptPasswordRevealToggle = {},
            onEncryptConfirmPasswordTyped = {},
            onEncryptPasswordConfirmRevealToggle = {},
            onEncryptSubmitClick = {},
            onUiMessageShown = {},
            onDeleteWalletClick = {},
            onDeleteWalletConfirm = {},
            onDeleteWalletDeny = {},
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}