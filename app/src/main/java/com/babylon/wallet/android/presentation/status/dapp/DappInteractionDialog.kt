package com.babylon.wallet.android.presentation.status.dapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DappInteractionDialog(
    modifier: Modifier = Modifier,
    viewModel: DappInteractionDialogViewModel,
    onBackPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DappInteractionDialogViewModel.Event.DismissDialog -> onBackPress()
            }
        }
    }

    val dismissHandler = {
        viewModel.onDismiss()
    }
    BackHandler(onBack = dismissHandler)
    BottomSheetWrapper(
        onDismissRequest = dismissHandler
    ) {
        DappInteractionDialogContent(
            modifier = modifier,
            state = state
        )
    }
}

@Composable
private fun DappInteractionDialogContent(
    modifier: Modifier = Modifier,
    state: DappInteractionDialogViewModel.State
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.transactionStatus_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = stringResource(id = R.string.dAppRequest_completion_subtitle, state.dAppName),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DappInteractionDialogPreview() {
    RadixWalletTheme {
        DappInteractionDialogContent(state = DappInteractionDialogViewModel.State(requestId = "abc", dAppName = "dApp"))
    }
}
