package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.onboarding.restore.backup.RestoreFromBackupViewModel.Event
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.formattedSpans
import com.babylon.wallet.android.utils.toDateString
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.launch
import rdx.works.core.sargon.isCompatible

@Composable
fun RestoreFromBackupScreen(
    viewModel: RestoreFromBackupViewModel,
    onBack: () -> Unit,
    onRestoreConfirmed: (Boolean) -> Unit,
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

    RestoreFromBackupContent(
        state = state,
        onBackClick = viewModel::onBackClick,
        turnOnCloudBackup = viewModel::turnOnCloudBackup,
        onRestoringProfileCheckChanged = viewModel::toggleRestoringProfileCheck,
        onRestoreFromFileClick = {
            openDocument.launch(arrayOf("*/*"))
        },
        onMessageShown = viewModel::onMessageShown,
        onPasswordTyped = viewModel::onPasswordTyped,
        onPasswordRevealToggle = viewModel::onPasswordRevealToggle,
        onPasswordSubmitted = viewModel::onPasswordSubmitted,
        onSubmitClick = viewModel::onSubmitClick,
        onOtherRestoreOptionsClick = onOtherRestoreOptionsClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is RestoreFromBackupViewModel.RestoreFromBackupEvent.OnDismiss -> onBack()
                is RestoreFromBackupViewModel.RestoreFromBackupEvent.OnRestoreConfirm -> onRestoreConfirmed(event.fromCloud)
                is RestoreFromBackupViewModel.RestoreFromBackupEvent.SignInToGoogle -> {
                    signInLauncher.launch(event.signInIntent)
                }
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
    turnOnCloudBackup: () -> Unit,
    onRestoringProfileCheckChanged: (Boolean) -> Unit,
    onRestoreFromFileClick: () -> Unit,
    onMessageShown: () -> Unit,
    onPasswordTyped: (String) -> Unit,
    onPasswordRevealToggle: () -> Unit,
    onPasswordSubmitted: () -> Unit,
    onSubmitClick: () -> Unit,
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
                    onClick = onSubmitClick,
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

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RadixSecondaryButton(
                    text = "sign in", // TODO when screen is ready
                    onClick = turnOnCloudBackup
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                Text(text = state.backupEmail)
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
private fun OtherRestoreOptionsSection(onOtherRestoreOptionsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault), color = RadixTheme.colors.gray4)
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
        HorizontalDivider(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault), color = RadixTheme.colors.gray4)
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
                restoringProfile = Profile.sample()
            ),
            onBackClick = {},
            turnOnCloudBackup = {},
            onRestoringProfileCheckChanged = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onSubmitClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}

@Preview
@Composable
fun RestoreFromBackupPreviewNoBackupExists() {
    RadixWalletTheme {
        RestoreFromBackupContent(
            state = RestoreFromBackupViewModel.State(
                restoringProfile = null
            ),
            onBackClick = {},
            turnOnCloudBackup = {},
            onRestoringProfileCheckChanged = {},
            onRestoreFromFileClick = {},
            onMessageShown = {},
            onPasswordTyped = {},
            onPasswordRevealToggle = {},
            onPasswordSubmitted = {},
            onSubmitClick = {},
            onOtherRestoreOptionsClick = {}
        )
    }
}
