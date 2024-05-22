package com.babylon.wallet.android.presentation.claimed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

@Composable
fun ClaimedByAnotherDeviceScreen(
    viewModel: ClaimedByAnotherDeviceViewModel,
    modifier: Modifier = Modifier,
    onNavigateToOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = false) {}

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ClaimedByAnotherDeviceViewModel.Event.ResetToOnboarding -> onNavigateToOnboarding()
                ClaimedByAnotherDeviceViewModel.Event.Dismiss -> onDismiss()
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.blue1)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {},
            confirmButton = {
                RadixTextButton(
                    text = "Claim Existing Wallet", // TODO Crowdin
                    onClick = viewModel::onReclaim,
                    contentColor =  RadixTheme.colors.blue2,
                    enabled = !state.isReclaiming
                )
            },
            dismissButton = {
                RadixTextButton(
                    text = "Clear Wallet on This Phone", // TODO Crowdin
                    onClick = viewModel::onResetWallet,
                    enabled = !state.isReclaiming
                )
            },
            title = {
                Text(text = "Claim this Wallet?") // TODO Crowdin
            },
            text = {
                Box {
                    Text(
                        modifier = Modifier.alpha(if (state.isReclaiming) 0.5f else 1f),
                        text = stringResource(id = R.string.splash_profileOnAnotherDeviceAlert_message),
                    )

                    if (state.isReclaiming) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = RadixTheme.colors.gray1
                        )
                    }
                }

            },
            shape = RadixTheme.shapes.roundedRectSmall,
            containerColor = RadixTheme.colors.defaultBackground
        )
    }
}
