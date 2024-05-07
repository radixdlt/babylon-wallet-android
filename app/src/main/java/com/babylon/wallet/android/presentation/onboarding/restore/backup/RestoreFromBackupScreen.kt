package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.formattedSpans
import com.babylon.wallet.android.utils.toDateString
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
fun RestoreFromBackupScreen(
    viewModel: RestoreFromBackupViewModel,
    onBackClick: () -> Unit,
    onRestoreConfirmed: (fromCloud: Boolean) -> Unit,
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

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleSignInResult(result)
    }

    val recoverDriveAuthLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handleAuthDriveResult(result)
        }

    RestoreFromBackupContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSignInToGoogleClick = viewModel::onSignInToGoogleClick,
        onRestoreFromFileClick = {
            openDocument.launch(arrayOf("*/*"))
        },
        onMessageShown = viewModel::onMessageShown,
        onPasswordTyped = viewModel::onPasswordTyped,
        onPasswordRevealToggle = viewModel::onPasswordRevealToggle,
        onPasswordSubmitted = viewModel::onPasswordSubmitted,
        onRestoringProfileCheckChanged = viewModel::toggleRestoringProfileCheck,
        onContinueClick = viewModel::onContinueClick,
        onOtherRestoreOptionsClick = onOtherRestoreOptionsClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent
            .collect { event ->
                when (event) {
                    is RestoreFromBackupViewModel.Event.OnDismiss -> onBackClick()

                    is RestoreFromBackupViewModel.Event.SignInToGoogle -> {
                        signInLauncher.launch(event.signInIntent)
                    }

                    is RestoreFromBackupViewModel.Event.RecoverUserAuthToDrive -> {
                        recoverDriveAuthLauncher.launch(event.authIntent)
                    }

                    is RestoreFromBackupViewModel.Event.OnRestoreConfirm -> onRestoreConfirmed(event.fromCloud)
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
    onSignInToGoogleClick: () -> Unit,
    onRestoreFromFileClick: () -> Unit,
    onMessageShown: () -> Unit,
    onPasswordTyped: (String) -> Unit,
    onPasswordRevealToggle: () -> Unit,
    onPasswordSubmitted: () -> Unit,
    onRestoringProfileCheckChanged: (Boolean) -> Unit,
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
        topBar = {
            RadixCenteredTopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = "",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                HorizontalDivider(color = RadixTheme.colors.gray5)

                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.common_continue),
                    onClick = onContinueClick,
                    enabled = state.isContinueEnabled
                )
            }
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
                .padding(padding)
                .verticalScroll(rememberScrollState()),
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

            if (state.isCloudBackupAuthorized.not()) {
                SignInToGoogleDrive(onSignInToGoogleClick = onSignInToGoogleClick)
            } else {
                RestoredProfilesList(
                    state = state,
                    onRestoringProfileCheckChanged = onRestoringProfileCheckChanged
                )
            }

            RadixTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.recoverProfileBackup_importFileButton_title),
                onClick = onRestoreFromFileClick
            )
            OtherRestoreOptionsSection(onOtherRestoreOptionsClick)
        }
    }

    if (state.isPasswordSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier,
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
private fun SignInToGoogleDrive(
    onSignInToGoogleClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            text = "Log in to Google Drive", // TODO Crowdin
            onClick = onSignInToGoogleClick
        )
    }
}

@Composable
private fun RestoredProfilesList(
    modifier: Modifier = Modifier,
    state: RestoreFromBackupViewModel.State,
    onRestoringProfileCheckChanged: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(state.restoringProfiles) { restoringProfile ->
            RestoredProfileListItem(
                modifier = Modifier.fillMaxSize(),
                restoringProfile = restoringProfile,
                isRestoringProfileChecked = state.isRestoringProfileChecked,
                onRestoringProfileCheckChanged = onRestoringProfileCheckChanged
            )
        }
    }
}

@Composable
private fun RestoredProfileListItem(
    modifier: Modifier = Modifier,
    restoringProfile: Profile,
    isRestoringProfileChecked: Boolean,
    onRestoringProfileCheckChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
        color = RadixTheme.colors.gray5,
        shadowElevation = 8.dp,
        shape = RadixTheme.shapes.roundedRectMedium
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    onRestoringProfileCheckChanged(!isRestoringProfileChecked)
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
                        restoringProfile.header.lastUsedOnDevice.description
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_lastModified,
                        restoringProfile.header.lastUsedOnDevice.date.toDateString()
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_numberOfAccounts,
                        restoringProfile.header.contentHint.numberOfAccountsOnAllNetworksInTotal.toInt()
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )

                Text(
                    text = stringResource(
                        id = R.string.recoverProfileBackup_numberOfPersonas,
                        restoringProfile.header.contentHint.numberOfPersonasOnAllNetworksInTotal.toInt()
                    ).formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    color = RadixTheme.colors.gray2,
                    style = RadixTheme.typography.body2Regular
                )
            }

            Checkbox(
                checked = isRestoringProfileChecked,
                onCheckedChange = onRestoringProfileCheckChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = RadixTheme.colors.gray1,
                    uncheckedColor = RadixTheme.colors.gray2,
                    checkmarkColor = Color.White
                )
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
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            color = RadixTheme.colors.gray4
        )
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
        modifier = modifier.navigationBarsPadding()
    ) {
        RadixCenteredTopAppBar(
            title = "",
            onBackClick = onBackClick
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

@UsesSampleValues
@Preview
@Composable
fun RestoreFromBackupPreviewBackupExists() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                backupEmail = "email",
                restoringProfiles = listOf(
                    Profile.sample()
                ).toImmutableList()
            ),
            onBackClick = {},
            onSignInToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileCheckChanged = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun RestoreFromBackupPreviewNoBackupExists() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(),
            onBackClick = {},
            onSignInToGoogleClick = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onRestoringProfileCheckChanged = {},
            onContinueClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}
