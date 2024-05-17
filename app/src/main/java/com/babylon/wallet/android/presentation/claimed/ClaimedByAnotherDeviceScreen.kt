package com.babylon.wallet.android.presentation.claimed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

@Composable
fun ClaimedByAnotherDeviceScreen(
    viewModel: ClaimedByAnotherDeviceViewModel,
    modifier: Modifier = Modifier,
    onNavigateToOnboarding: () -> Unit
) {
    BackHandler(enabled = false) {}

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ClaimedByAnotherDeviceViewModel.Event.ResetToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.blue1)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                if (accepted) {
                    viewModel.onModalAcknowledged()
                }
            },
            message = {
                Text(text = "The profile was claimed by another device") // TODO crowdin
            },
            dismissText = null
        )
    }
}