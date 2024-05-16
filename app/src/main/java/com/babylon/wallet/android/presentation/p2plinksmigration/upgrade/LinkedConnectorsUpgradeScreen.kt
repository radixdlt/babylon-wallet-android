package com.babylon.wallet.android.presentation.p2plinksmigration.upgrade

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.linkedconnector.LinkedConnectorMessageScreen

@Composable
fun LinkedConnectorsUpgradeScreen(
    onContinueClick: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: LinkedConnectorsUpgradeViewModel = hiltViewModel()
) {
    LinkedConnectorsUpgradeContent(
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
private fun LinkedConnectorsUpgradeContent(
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
            LinkedConnectorMessageScreen(
                title = stringResource(id = R.string.linkedConnectors_relink_title),
                message = stringResource(id = R.string.linkedConnectors_upgrade_message),
                onPositiveClick = onContinueClick,
                onNegativeClick = onDismiss,
                negativeButton = stringResource(id = R.string.linkedConnectors_relink_later_btn)
            )
        }
    }
}

@Composable
@Preview
private fun LinkedConnectorsUpgradePreview() {
    RadixWalletTheme {
        LinkedConnectorsUpgradeContent(
            onContinueClick = {},
            onDismiss = {}
        )
    }
}