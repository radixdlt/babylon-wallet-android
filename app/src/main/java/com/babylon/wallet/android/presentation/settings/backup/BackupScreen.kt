@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.NotBackedUpWarning
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.BackupState

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    onSystemBackupSettingsClick: () -> Unit,
    onProfileDeleted: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackupScreenContent(
        modifier = modifier,
        state = state,
        onBackupCheckChanged = { isChecked ->
            viewModel.onBackupSettingChanged(isChecked)
        },
        onSystemBackupSettingsClick = onSystemBackupSettingsClick,
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
        onBackClick = viewModel::onBackClick
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType = "application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.onFileChosen(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is BackupViewModel.Event.Dismiss -> onClose()
                is BackupViewModel.Event.ChooseExportFile -> filePickerLauncher.launch(it.fileName)
                is BackupViewModel.Event.ProfileDeleted -> onProfileDeleted()
            }
        }
    }
}

@Composable
private fun BackupScreenContent(
    modifier: Modifier = Modifier,
    state: BackupViewModel.State,
    onBackupCheckChanged: (Boolean) -> Unit,
    onSystemBackupSettingsClick: () -> Unit,
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
    onBackClick: () -> Unit
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
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    SyncSheetState(
        bottomSheetState = modalBottomSheetState,
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
        }
    ) {
        Scaffold(
            topBar = {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.settings_backups),
                    onBackClick = onBackClick
                )
            },
            snackbarHost = {
                RadixSnackbarHost(
                    hostState = snackBarHostState,
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
                )
            },
            containerColor = RadixTheme.colors.gray5
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.profileBackup_headerTitle)
                        .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body1HighImportance
                )

                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.profileBackup_automaticBackups_title),
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
                        SwitchSettingsItem(
                            titleRes = if (state.isBackupEnabled) {
                                R.string.androidProfileBackup_automaticBackups_disable
                            } else {
                                R.string.androidProfileBackup_automaticBackups_enable
                            },
                            subtitleRes = R.string.androidProfileBackup_automaticBackups_subtitle,
                            checked = state.isBackupEnabled,
                            icon = {
                                Icon(
                                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_backup),
                                    tint = RadixTheme.colors.gray1,
                                    contentDescription = null
                                )
                            },
                            onCheckedChange = onBackupCheckChanged
                        )

                        NotBackedUpWarning(
                            backupState = state.backupState,
                            horizontalSpacing = RadixTheme.dimensions.paddingMedium
                        )

                        if (isBackupScreenNavigationSupported()) {
                            RadixSecondaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.androidProfileBackup_openSystemBackupSettings),
                                onClick = onSystemBackupSettingsClick
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.profileBackup_manualBackups_title),
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
                            text = stringResource(id = R.string.profileBackup_manualBackups_subtitle)
                                .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.Bold)),
                            color = RadixTheme.colors.gray2,
                            style = RadixTheme.typography.body1HighImportance
                        )

                        RadixSecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.profileBackup_manualBackups_exportButtonTitle),
                            onClick = onFileBackupClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                Text(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.profileBackup_deleteWallet_title),
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

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onDeleteWalletClick,
                            shape = RadixTheme.shapes.roundedRectSmall,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color.White,
                                containerColor = RadixTheme.colors.red1
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.androidProfileBackup_deleteWallet_confirmButton),
                                style = RadixTheme.typography.body1Header,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
    }
}

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
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXSmall),
                    text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogConfirm),
                    color = RadixTheme.colors.red1
                )

                Text(
                    modifier = modifier
                        .clickable(role = Role.Button) { onConfirm(false) }
                        .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXSmall),
                    text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupDialogDeny),
                    color = RadixTheme.colors.red1
                )
            }
        },
        dismissButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onDeny() }
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXSmall),
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
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXSmall),
                text = stringResource(id = R.string.profileBackup_deleteWalletDialog_confirm),
                color = RadixTheme.colors.red1
            )
        },
        dismissButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onDeny() }
                    .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingXSmall),
                text = stringResource(id = R.string.common_cancel),
                color = RadixTheme.colors.blue2
            )
        }
    )
}

@Composable
private fun SyncSheetState(
    bottomSheetState: ModalBottomSheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { bottomSheetState.show() }
        } else {
            scope.launch { bottomSheetState.hide() }
        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
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
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupTitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
                text = stringResource(id = R.string.profileBackup_manualBackups_encryptBackupSubtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
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
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
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
            onSystemBackupSettingsClick = {},
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
            onBackClick = {}
        )
    }
}
