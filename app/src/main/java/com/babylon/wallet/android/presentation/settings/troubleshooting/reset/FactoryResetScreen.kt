@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.settings.troubleshooting.reset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.RecoverableStatusCard
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningButton

@Composable
fun FactoryResetScreen(
    viewModel: FactoryResetViewModel,
    modifier: Modifier = Modifier,
    onProfileDeleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FactoryResetScreenContent(
        modifier = modifier,
        state = state,
        onUiMessageShown = viewModel::onMessageShown,
        onDeleteWalletClick = viewModel::onDeleteWalletClick,
        onDeleteWalletConfirm = viewModel::onDeleteWalletConfirm,
        onDeleteWalletDeny = viewModel::onDeleteWalletDeny,
        onBackClick = onBackClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is FactoryResetViewModel.Event.ProfileDeleted -> {
                    onProfileDeleted()
                }
            }
        }
    }
}

@Composable
private fun FactoryResetScreenContent(
    modifier: Modifier = Modifier,
    state: FactoryResetViewModel.State,
    onUiMessageShown: () -> Unit,
    onDeleteWalletClick: () -> Unit,
    onDeleteWalletConfirm: () -> Unit,
    onDeleteWalletDeny: () -> Unit,
    onBackClick: () -> Unit
) {
    if (state.deleteWalletDialogVisible) {
        DeleteWalletDialog(
            onConfirm = onDeleteWalletConfirm,
            onDeny = onDeleteWalletDeny
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
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.factoryReset_title),
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
        containerColor = RadixTheme.colors.gray4,
        bottomBar = {
            Column(modifier = Modifier.background(RadixTheme.colors.defaultBackground)) {
                WarningButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(R.string.factoryReset_resetWallet),
                    onClick = onDeleteWalletClick
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.factoryReset_message),
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1Header
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Column(
                modifier = Modifier
                    .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.factoryReset_status),
                    color = RadixTheme.colors.gray1,
                    style = RadixTheme.typography.body1Header
                )
                if (state.securityProblems?.isEmpty() == true) {
                    RecoverableStatusCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.factoryReset_recoverable)
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                } else {
                    UnrecoverableStatusCard(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.factoryReset_unrecoverable_title)
                    )
                    Text(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.factoryReset_unrecoverable_message),
                        color = RadixTheme.colors.orange1,
                        style = RadixTheme.typography.body1Link
                    )
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
                        text = stringResource(id = R.string.factoryReset_disclosure),
                        color = RadixTheme.colors.gray1,
                        style = RadixTheme.typography.body1Regular
                    )
                }
            }
        }
    }
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
        title = {
            Text(
                text = stringResource(id = R.string.factoryReset_dialog_title),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.factoryReset_dialog_message),
                style = RadixTheme.typography.body1Regular,
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
                text = stringResource(id = R.string.common_confirm),
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

@Composable
fun UnrecoverableStatusCard(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.orange1, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(painter = painterResource(id = DSR.ic_security_center), contentDescription = null, tint = RadixTheme.colors.white)
        Text(
            text = text,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.white
        )
    }
}

@Preview
@Composable
private fun FactoryResetScreenPreview(
    @PreviewParameter(ResetWalletViewModelStateProvider::class) state: FactoryResetViewModel.State
) {
    RadixWalletTheme {
        FactoryResetScreenContent(
            state = state,
            onUiMessageShown = {},
            onDeleteWalletClick = {},
            onDeleteWalletConfirm = {},
            onDeleteWalletDeny = {},
            onBackClick = {}
        )
    }
}

class ResetWalletViewModelStateProvider : PreviewParameterProvider<FactoryResetViewModel.State> {
    override val values: Sequence<FactoryResetViewModel.State>
        get() = sequenceOf(
            FactoryResetViewModel.State(),
            FactoryResetViewModel.State(securityProblems = emptySet())
        )
}
