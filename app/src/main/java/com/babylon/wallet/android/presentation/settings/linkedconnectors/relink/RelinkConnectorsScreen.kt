package com.babylon.wallet.android.presentation.settings.linkedconnectors.relink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.linkedconnector.LinkedConnectorMessageScreen

@Composable
fun RelinkConnectorsScreen(
    onContinueClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: RelinkConnectorsViewModel = hiltViewModel()
) {
    RelinkConnectorsContent(
        state = viewModel.state.collectAsState().value,
        onContinueClick = {
            viewModel.acknowledgeMessage()
            onContinueClick()
        },
        onDismiss = {
            viewModel.acknowledgeMessage()
            onDismiss()
        }
    )
}

@Composable
private fun RelinkConnectorsContent(
    state: RelinkConnectorsViewModel.UiState,
    onContinueClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            when (state) {
                RelinkConnectorsViewModel.UiState.AppUpdate -> LinkedConnectorMessageScreen(
                    title = stringResource(id = R.string.linkedConnectors_relinkConnectors_title),
                    message = stringResource(id = R.string.linkedConnectors_relinkConnectors_afterUpdateMessage),
                    onPositiveClick = onContinueClick,
                    onNegativeClick = onDismiss,
                    negativeButton = stringResource(id = R.string.linkedConnectors_relinkConnectors_laterButton)
                )
                RelinkConnectorsViewModel.UiState.ProfileRestore -> LinkedConnectorMessageScreen(
                    title = stringResource(id = R.string.linkedConnectors_relinkConnectors_title),
                    message = stringResource(id = R.string.linkedConnectors_relinkConnectors_afterProfileRestoreMessage),
                    onPositiveClick = onContinueClick,
                    onNegativeClick = onDismiss,
                    negativeButton = stringResource(id = R.string.linkedConnectors_relinkConnectors_laterButton)
                )
                RelinkConnectorsViewModel.UiState.Idle -> {}
            }
        }
    }
}

@Composable
@Preview
private fun RelinkConnectorsPreview(
    @PreviewParameter(RelinkConnectorsPreviewProvider::class) state: RelinkConnectorsViewModel.UiState
) {
    RadixWalletTheme {
        RelinkConnectorsContent(
            state = state,
            onContinueClick = {},
            onDismiss = {}
        )
    }
}

class RelinkConnectorsPreviewProvider : PreviewParameterProvider<RelinkConnectorsViewModel.UiState> {

    override val values: Sequence<RelinkConnectorsViewModel.UiState>
        get() = sequenceOf(
            RelinkConnectorsViewModel.UiState.AppUpdate,
            RelinkConnectorsViewModel.UiState.ProfileRestore
        )
}
