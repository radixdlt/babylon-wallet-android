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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.backup.BackupViewModel.State.EncryptSheet
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.rememberLauncherForSignInToGoogle
import rdx.works.core.InstantGenerator
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.domain.cloudbackup.BackupWarning

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
        onBackupCheckChanged = viewModel::onBackupSettingChanged,
        onFileBackupClick = viewModel::onFileBackupClick,
        onFileBackupConfirm = viewModel::onFileBackupConfirm,
        onFileBackupDeny = viewModel::onFileBackupDeny,
        onEncryptPasswordTyped = viewModel::onEncryptPasswordTyped,
        onEncryptPasswordRevealToggle = viewModel::onEncryptPasswordRevealChange,
        onEncryptConfirmPasswordTyped = viewModel::onEncryptConfirmPasswordTyped,
        onEncryptPasswordConfirmRevealToggle = viewModel::onEncryptConfirmPasswordRevealChange,
        onEncryptSubmitClick = viewModel::onEncryptSubmitClick,
        onUiMessageShown = viewModel::onMessageShown,
        onDisconnectClick = viewModel::onDisconnectClick,
        onBackClick = viewModel::onBackClick,
    )

    val signInLauncher = rememberLauncherForSignInToGoogle(viewModel = viewModel)

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
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is BackupViewModel.Event.Dismiss -> onClose()
                is BackupViewModel.Event.ChooseExportFile -> filePickerLauncher.launch(event.fileName)
                is BackupViewModel.Event.ProfileDeleted -> onProfileDeleted()
                is BackupViewModel.Event.DeleteFile -> {
                    DocumentsContract.deleteDocument(context.contentResolver, event.file)
                }
                is BackupViewModel.Event.SignInToGoogle -> signInLauncher.launch(Unit)
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

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onUiMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.configurationBackup_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.configurationBackup_heading),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1Header
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            if (state.backupState.isNotUpdated) {
                state.backupState.backupWarning?.let {
                    BackupWarning(backupWarning = it)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
            Column(
                modifier = Modifier
                    .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium)
            ) {
                BackupStatusCard(
                    backupState = state.backupState,
                    isCloudAuthorizationInProgress = state.isCloudAuthorizationInProgress,
                    onBackupCheckChanged = onBackupCheckChanged
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                if (state.backupState.isAuthorized) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                        color = RadixTheme.colors.gray4
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    LoggedInStatus(
                        email = state.backupState.email.orEmpty(),
                        onDisconnectClick = onDisconnectClick
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectBottomMedium)
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingDefault
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
                        text = stringResource(id = R.string.configurationBackup_automated_warning),
                        color = RadixTheme.colors.gray1,
                        style = RadixTheme.typography.body1Regular
                    )
                }
            }

            Text(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.configurationBackup_manual_heading),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1Header
            )

            ManualBackupCard(
                backupState = state.backupState,
                onFileBackupClick = onFileBackupClick
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }

    if (state.encryptSheet is EncryptSheet.Open) {
        BottomSheetDialogWrapper(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBarsAndBanner)
                .imePadding()
                .navigationBarsPadding(),
            addScrim = true,
            showDragHandle = true,
            headerBackIcon = Icons.AutoMirrored.Filled.ArrowBack,
            showDefaultTopBar = true,
            onDismiss = onBackClick,
        ) {
            EncryptSheet(
                state = state.encryptSheet,
                onPasswordTyped = onEncryptPasswordTyped,
                onPasswordRevealToggle = onEncryptPasswordRevealToggle,
                onPasswordConfirmTyped = onEncryptConfirmPasswordTyped,
                onPasswordConfirmRevealToggle = onEncryptPasswordConfirmRevealToggle,
                onSubmitClick = onEncryptSubmitClick
            )
        }
    }
}

@Composable
private fun ManualBackupCard(
    modifier: Modifier = Modifier,
    backupState: BackupState,
    onFileBackupClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium)
    ) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.configurationBackup_manual_text),
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular
        )
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.configurationBackup_manual_exportButton),
            onClick = onFileBackupClick
        )
        backupState.lastManualBackupLabel?.let {
            Text(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    top = RadixTheme.dimensions.paddingSmall
                ),
                text = stringResource(
                    id = R.string.configurationBackup_automated_lastBackup,
                    backupState.lastManualBackupLabel ?: stringResource(
                        id = R.string.common_none
                    )
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
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
                text = stringResource(id = R.string.configurationBackup_manual_warning),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Regular
            )
        }
    }
}

@Composable
private fun BackupStatusCard(
    modifier: Modifier = Modifier,
    isCloudAuthorizationInProgress: Boolean,
    backupState: BackupState,
    onBackupCheckChanged: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
    ) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        SwitchSettingsItem(
            titleRes = R.string.configurationBackup_automated_toggleAndroid,
            checked = backupState.isCloudBackupEnabled,
            icon = {
                Icon(
                    painter = painterResource(id = DSR.ic_backup),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = null
                )
            },
            onCheckedChange = onBackupCheckChanged,
            isLoading = isCloudAuthorizationInProgress
        )
        if (backupState.isCloudBackupNotUpdated) {
            Text(
                modifier = Modifier.padding(start = 44.dp),
                text = stringResource(
                    id = R.string.configurationBackup_automated_lastBackup,
                    backupState.lastCloudBackupLabel ?: stringResource(
                        id = R.string.common_none
                    )
                ),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingSemiLarge),
            color = RadixTheme.colors.gray4
        )
        Text(
            text = stringResource(id = R.string.configurationBackup_automated_text),
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_automated_accountsItemTitle),
            subtitle = stringResource(id = R.string.configurationBackup_automated_accountsItemSubtitle),
            backupState = backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_automated_personasItemTitle),
            subtitle = stringResource(id = R.string.configurationBackup_automated_personasItemSubtitle),
            backupState = backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_automated_securityFactorsItemTitle),
            subtitle = stringResource(id = R.string.configurationBackup_automated_securityFactorsItemSubtitle),
            backupState = backupState
        )
        BackupStatusSection(
            title = stringResource(id = R.string.configurationBackup_automated_walletSettingsItemTitle),
            subtitle = stringResource(id = R.string.configurationBackup_automated_walletSettingsItemSubtitle),
            backupState = backupState
        )
    }
}

@Composable
private fun LoggedInStatus(
    modifier: Modifier = Modifier,
    email: String,
    onDisconnectClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.configurationBackup_automated_loggedInAsAndroid),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body2Regular
            )
            Text(
                text = email,
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RadixTextButton(
            modifier = Modifier,
            text = stringResource(id = R.string.configurationBackup_automated_disconnectAndroid),
            isWithoutPadding = true,
            fontSize = 14.sp,
            onClick = onDisconnectClick
        )
    }
}

@Composable
private fun BackupWarning(
    modifier: Modifier = Modifier,
    backupWarning: BackupWarning
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.lightOrange, RadixTheme.shapes.roundedRectMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.lightOrange, RadixTheme.shapes.roundedRectMedium)
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingMedium
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_warning_error),
                contentDescription = null,
                tint = RadixTheme.colors.orange3
            )
            val warningText = when (backupWarning) {
                BackupWarning.CLOUD_BACKUP_SERVICE_ERROR -> stringResource(id = R.string.securityProblems_no5_configurationBackup)
                BackupWarning.CLOUD_BACKUP_DISABLED_WITH_NO_MANUAL_BACKUP -> stringResource(
                    id = R.string.securityProblems_no6_configurationBackup
                )
                BackupWarning.CLOUD_BACKUP_DISABLED_WITH_OUTDATED_MANUAL_BACKUP -> stringResource(
                    id = R.string.securityProblems_no7_configurationBackup
                )
            }
            Text(
                text = warningText,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.orange3
            )
        }
    }
}

@Composable
private fun BackupStatusSection(
    title: String,
    subtitle: String,
    backupState: BackupState
) {
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
        val statusColor = if (backupState.isCloudBackupNotUpdated) RadixTheme.colors.orange3 else RadixTheme.colors.green1

        PromptLabel(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            textColor = statusColor,
            iconRes = remember(backupState.isCloudBackupNotUpdated) {
                if (backupState.isCloudBackupNotUpdated) DSR.ic_warning_error else DSR.ic_check_circle
            },
            iconTint = statusColor,
            endContent = {
                Icon(
                    painter = painterResource(id = if (expanded) DSR.ic_arrow_up else DSR.ic_arrow_down),
                    tint = RadixTheme.colors.gray2,
                    contentDescription = null
                )
            }
        )

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
                            vertical = RadixTheme.dimensions.paddingXXSmall
                        ),
                    text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogConfirm),
                    color = RadixTheme.colors.red1
                )

                Text(
                    modifier = modifier
                        .clickable(role = Role.Button) { onConfirm(false) }
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingSmall,
                            vertical = RadixTheme.dimensions.paddingXXSmall
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
                        vertical = RadixTheme.dimensions.paddingXXSmall
                    ),
                text = stringResource(id = R.string.common_cancel),
                color = RadixTheme.colors.blue2
            )
        }
    )
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
) {
    Scaffold(
        modifier = modifier,
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .widthIn(max = 300.dp)
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
                hintColor = RadixTheme.colors.gray2,
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
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (state.isPasswordRevealed) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

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
                hintColor = RadixTheme.colors.gray2,
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

@Preview(showBackground = true)
@Composable
fun BackupStatusCardPreview() {
    RadixWalletPreviewTheme {
        BackupStatusCard(
            backupState = BackupState.CloudBackupEnabled(email = "my cool email"),
            isCloudAuthorizationInProgress = false,
            onBackupCheckChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BackupStatusCarAuthInProgressPreview() {
    RadixWalletPreviewTheme {
        BackupStatusCard(
            backupState = BackupState.CloudBackupEnabled(email = "my cool email"),
            isCloudAuthorizationInProgress = true,
            onBackupCheckChanged = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ManualBackupStatusCardPreview() {
    RadixWalletPreviewTheme {
        ManualBackupCard(
            backupState = BackupState.CloudBackupDisabled(
                email = "my cool email",
                lastCloudBackupTime = null,
                lastManualBackupTime = InstantGenerator(),
                lastModifiedProfileTime = TimestampGenerator()
            ),
            onFileBackupClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoggedInStatusPreview() {
    RadixWalletPreviewTheme {
        LoggedInStatus(
            modifier = Modifier.height(40.dp),
            email = "averylongemail@withlongmail.com",
            onDisconnectClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BackupStatusSectionPreview() {
    RadixWalletPreviewTheme {
        BackupStatusSection(
            title = "title",
            subtitle = "subtitle",
            backupState = BackupState.CloudBackupEnabled(email = "email@mail.com")
        )
    }
}

@Preview
@Composable
fun BackupScreenPreview() {
    RadixWalletTheme {
        BackupScreenContent(
            state = BackupViewModel.State(backupState = BackupState.CloudBackupEnabled(email = "email@mail.com")),
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
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}

// refer to this table
// https://radixdlt.atlassian.net/wiki/spaces/AT/pages/3392569357/Security-related+Problem+States+in+the+Wallet
@Preview(showBackground = true)
@Composable
fun BackupSecurityProblem5Preview() {
    RadixWalletPreviewTheme {
        BackupWarning(backupWarning = BackupWarning.CLOUD_BACKUP_SERVICE_ERROR)
    }
}

@Preview(showBackground = true)
@Composable
fun BackupSecurityProblem6Preview() {
    RadixWalletPreviewTheme {
        BackupWarning(backupWarning = BackupWarning.CLOUD_BACKUP_DISABLED_WITH_NO_MANUAL_BACKUP)
    }
}

@Preview(showBackground = true)
@Composable
fun BackupSecurityProblem7Preview() {
    RadixWalletPreviewTheme {
        BackupWarning(backupWarning = BackupWarning.CLOUD_BACKUP_DISABLED_WITH_OUTDATED_MANUAL_BACKUP)
    }
}

@Preview
@Composable
fun BackupDisabledAndNotUpdatedManualBackupPreview() {
    RadixWalletTheme {
        val now = TimestampGenerator()
        val oneDayBefore = now.minusDays(1)
        BackupScreenContent(
            state = BackupViewModel.State(
                backupState = BackupState.CloudBackupDisabled(
                    email = "my cool email",
                    lastCloudBackupTime = now,
                    lastManualBackupTime = oneDayBefore.toInstant(),
                    lastModifiedProfileTime = now
                )
            ),
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
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}

@Preview
@Composable
fun BackupDisabledAndNeverManualBackupPreview() {
    RadixWalletTheme {
        val now = TimestampGenerator()
        BackupScreenContent(
            state = BackupViewModel.State(
                backupState = BackupState.CloudBackupDisabled(
                    email = "my cool email",
                    lastCloudBackupTime = now,
                    lastManualBackupTime = null,
                    lastModifiedProfileTime = now
                )
            ),
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
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}

@Preview
@Composable
fun BackupDisabledAndUpdatedManualBackupPreview() {
    RadixWalletTheme {
        val now = TimestampGenerator()
        val oneDayBefore = now.minusDays(1)
        BackupScreenContent(
            state = BackupViewModel.State(
                backupState = BackupState.CloudBackupDisabled(
                    email = "my cool email",
                    lastCloudBackupTime = oneDayBefore,
                    lastManualBackupTime = now.toInstant(),
                    lastModifiedProfileTime = now
                )
            ),
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
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}

@Preview
@Composable
fun BackupHasErrorAndUpdatedManualBackupPreview() {
    RadixWalletTheme {
        val now = TimestampGenerator()
        val oneDayBefore = now.minusDays(1)
        BackupScreenContent(
            state = BackupViewModel.State(
                backupState = BackupState.CloudBackupEnabled(
                    email = "my cool email",
                    hasAnyErrors = true,
                    lastCloudBackupTime = oneDayBefore,
                    lastManualBackupTime = now.toInstant(),
                    lastModifiedProfileTime = now
                )
            ),
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
            onBackClick = {},
            onDisconnectClick = {}
        )
    }
}
