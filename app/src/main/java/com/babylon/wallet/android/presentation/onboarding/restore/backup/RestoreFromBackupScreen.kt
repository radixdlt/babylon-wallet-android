@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans
import com.babylon.wallet.android.utils.rememberLauncherForSignInToGoogle
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.domain.backup.BackupType
import rdx.works.profile.domain.backup.CloudBackupFileEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RestoreFromBackupScreen(
    viewModel: RestoreFromBackupViewModel,
    onBackClick: () -> Unit,
    onRestoreConfirmed: (backupType: BackupType) -> Unit,
    onOtherRestoreOptionsClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onRestoreFromFile(uri)
        }
    }

    val signInLauncher = rememberLauncherForSignInToGoogle(viewModel = viewModel)

    RestoreFromBackupContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onLoginToGoogleClick = viewModel::onLoginToGoogleClick,
        onRestoreFromFileClick = {
            openDocument.launch(arrayOf("*/*"))
        },
        onMessageShown = viewModel::onMessageShown,
        onPasswordTyped = viewModel::onPasswordTyped,
        onPasswordRevealToggle = viewModel::onPasswordRevealToggle,
        onPasswordSubmitted = viewModel::onPasswordSubmitted,
        onRestoringProfileSelected = viewModel::onRestoringProfileSelected,
        onContinueClick = viewModel::onContinueClick,
        onOtherRestoreOptionsClick = onOtherRestoreOptionsClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent
            .collect { event ->
                when (event) {
                    is RestoreFromBackupViewModel.Event.OnDismiss -> onBackClick()

                    is RestoreFromBackupViewModel.Event.SignInToGoogle -> signInLauncher.launch(Unit)

                    is RestoreFromBackupViewModel.Event.OnRestoreConfirmed -> onRestoreConfirmed(event.backupType)
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreFromBackupContent(
    modifier: Modifier = Modifier,
    state: RestoreFromBackupViewModel.State,
    onBackClick: () -> Unit,
    onLoginToGoogleClick: () -> Unit,
    onRestoreFromFileClick: () -> Unit,
    onMessageShown: () -> Unit,
    onPasswordTyped: (String) -> Unit,
    onPasswordRevealToggle: () -> Unit,
    onPasswordSubmitted: () -> Unit,
    onRestoringProfileSelected: (RestoreFromBackupViewModel.State.RestoringProfile) -> Unit,
    onContinueClick: () -> Unit,
    onOtherRestoreOptionsClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    SyncSheetState(
        state = modalBottomSheetState,
        isSheetVisible = state.isPasswordSheetVisible,
        onSheetClosed = {
            if (state.isPasswordSheetVisible) {
                onBackClick()
            }
        }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                windowInsets = WindowInsets.statusBarsAndBanner,
                title = "",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = R.string.common_continue),
                isLoading = state.isDownloadingSelectedCloudBackup,
                enabled = state.isContinueEnabled
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverProfileBackup_header_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverProfileBackup_header_subtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = stringResource(id = R.string.androidRecoverProfileBackup_choose_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (!state.isCloudBackupAuthorized) {
                    item {
                        SignInToGoogleDrive(
                            isAccessToGoogleDriveInProgress = state.isAccessToGoogleDriveInProgress,
                            onLoginToGoogleClick = onLoginToGoogleClick
                        )
                    }
                } else {
                    if (state.restoringProfiles == null) {
                        item {
                            LoadingCloudBackups()
                        }
                    } else if (state.restoringProfiles.isEmpty()) {
                        item {
                            EmptyCloudBackups()
                        }
                    } else {
                        items(state.restoringProfiles) { restoringProfile ->
                            RestoredProfileListItem(
                                modifier = Modifier.fillMaxSize(),
                                restoringProfile = restoringProfile.data,
                                isRestoringProfileSelected = restoringProfile.selected,
                                onRadioButtonClick = { onRestoringProfileSelected(restoringProfile.data) }
                            )
                        }
                    }
                }

                item {
                    RadixTextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.recoverProfileBackup_importFileButton_title),
                        onClick = onRestoreFromFileClick
                    )
                }

                item {
                    OtherRestoreOptionsSection(onOtherRestoreOptionsClick)
                }
            }
        }
    }

    if (state.isPasswordSheetVisible) {
        DefaultModalSheetLayout(
            sheetState = modalBottomSheetState,
            enableImePadding = true,
            wrapContent = true,
            sheetContent = {
                if (state.passwordSheetState is RestoreFromBackupViewModel.State.PasswordSheet.Open) {
                    PasswordSheet(
                        state = state.passwordSheetState,
                        onBackClick = onBackClick,
                        onPasswordTyped = onPasswordTyped,
                        onPasswordRevealToggle = onPasswordRevealToggle,
                        onPasswordSubmitted = onPasswordSubmitted
                    )
                }
            },
            onDismissRequest = onBackClick
        )
    }
}

@Composable
private fun EmptyCloudBackups() {
    Box(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault)
            .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.androidRecoverProfileBackup_noBackupsAvailable),
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.secondaryHeader
        )
    }
}

@Composable
private fun LoadingCloudBackups() {
    Box(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = RadixTheme.colors.gray1)
    }
}

@Composable
private fun SignInToGoogleDrive(
    isAccessToGoogleDriveInProgress: Boolean,
    onLoginToGoogleClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
            text = stringResource(id = R.string.onboarding_cloudRestoreAndroid_loginButton),
            isLoading = isAccessToGoogleDriveInProgress,
            onClick = onLoginToGoogleClick
        )
    }
}

@Composable
private fun RestoredProfileListItem(
    modifier: Modifier = Modifier,
    restoringProfile: RestoreFromBackupViewModel.State.RestoringProfile,
    isRestoringProfileSelected: Boolean,
    onRadioButtonClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(bottom = RadixTheme.dimensions.paddingMedium),
        color = RadixTheme.colors.gray5,
        shadowElevation = 8.dp,
        shape = RadixTheme.shapes.roundedRectMedium
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    onRadioButtonClick()
                }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = RadixTheme.dimensions.paddingXSmall)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_backupFrom,
                        if (restoringProfile.isBackedUpByTheSameDevice) {
                            stringResource(id = R.string.iOSProfileBackup_thisDevice)
                        } else {
                            restoringProfile.deviceDescription
                        }
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_lastModified,
                        restoringProfile.lastModified.displayable()
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_numberOfAccounts,
                        restoringProfile.totalNumberOfAccountsOnAllNetworks
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_numberOfPersonas,
                        restoringProfile.totalNumberOfPersonasOnAllNetworks
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )
            }

            RadioButton(
                selected = isRestoringProfileSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RadixTheme.colors.gray1,
                    unselectedColor = RadixTheme.colors.gray2,
                ),
                onClick = onRadioButtonClick
            )
        }
    }
}

@Composable
private fun OtherRestoreOptionsSection(onOtherRestoreOptionsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            color = RadixTheme.colors.gray4
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                ),
            text = stringResource(id = R.string.recoverProfileBackup_backupNotAvailable),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        RadixTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.recoverProfileBackup_otherRestoreOptionsButton),
            onClick = onOtherRestoreOptionsClick
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSheetState(
    state: SheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { state.show() }
        } else {
            scope.launch { state.hide() }
        }
    }

    LaunchedEffect(state.isVisible) {
        if (!state.isVisible) {
            onSheetClosed()
        }
    }
}

@Composable
private fun PasswordSheet(
    modifier: Modifier = Modifier,
    state: RestoreFromBackupViewModel.State.PasswordSheet.Open,
    onBackClick: () -> Unit,
    onPasswordTyped: (String) -> Unit,
    onPasswordRevealToggle: () -> Unit,
    onPasswordSubmitted: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        RadixCenteredTopAppBar(
            title = "",
            onBackClick = onBackClick,
        )

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            onValueChanged = onPasswordTyped,
            value = state.password,
            hint = stringResource(id = R.string.encryptProfileBackup_enterPasswordField_placeholder),
            error = if (state.isPasswordInvalid) {
                stringResource(id = R.string.recoverProfileBackup_passwordWrong)
            } else {
                null
            },
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
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Password
            ),
            visualTransformation = if (state.isPasswordRevealed) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        HorizontalDivider(color = RadixTheme.colors.gray5)

        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.common_continue),
            onClick = onPasswordSubmitted,
            enabled = state.isSubmitEnabled
        )
    }
}

@Composable
private fun Timestamp.displayable() = remember(this) {
    val formatter = DateTimeFormatter.ofPattern(CloudBackupFileEntity.LAST_USED_DATE_FORMAT_SHORT_MONTH).withZone(ZoneId.systemDefault())
    formatter.format(this)
}

@UsesSampleValues
@Preview
@Composable
fun RestoreFromBackupWithMultipleCloudBackupsPreview() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                backupEmail = "email",
                restoringProfiles = CloudBackupFileEntity.sample.all.map {
                    Selectable<RestoreFromBackupViewModel.State.RestoringProfile>(
                        data = RestoreFromBackupViewModel.State.RestoringProfile.GoogleDrive(it, false)
                    )
                }.toPersistentList()
            ),
            onBackClick = {},
            onLoginToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileSelected = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun RestoreFromBackupNotLoggedInPreview() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(),
            onBackClick = {},
            onLoginToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileSelected = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}

@Preview
@Composable
fun RestoreFromBackupLoadingProfilesPreview() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                backupEmail = "email",
                restoringProfiles = null
            ),
            onBackClick = {},
            onLoginToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileSelected = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun RestoreFromBackupLoadingProfilesPreview2() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                isAccessToGoogleDriveInProgress = false,
                isDownloadingSelectedCloudBackup = true,
                backupEmail = "email",
                restoringProfiles = listOf(
                    Selectable<RestoreFromBackupViewModel.State.RestoringProfile>(
                        data = RestoreFromBackupViewModel.State.RestoringProfile.GoogleDrive(
                            entity = CloudBackupFileEntity.sample(),
                            isBackedUpByTheSameDevice = false
                        )
                    )
                ).toImmutableList()
            ),
            onBackClick = {},
            onLoginToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileSelected = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}
