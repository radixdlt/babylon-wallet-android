package com.babylon.wallet.android.presentation.m2m

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.utils.openUrl

@Composable
fun M2MScreen(
    modifier: Modifier = Modifier,
    viewModel: M2MViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is M2MViewModel.Event.OpenUrl -> {
                    context.openUrl(event.url)
                    onBackClick()
                }
                M2MViewModel.Event.Close -> onBackClick()
            }
        }
    }
    M2MContent(modifier.fillMaxSize(), state)
}

@Composable
fun M2MContent(
    modifier: Modifier,
    state: State,
) {
    Box(modifier = modifier.padding(RadixTheme.dimensions.paddingDefault)) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium, Alignment.CenterVertically)
        ) {
            Text(
                text = "Origin: ${state.dappLink?.origin}",
                style = RadixTheme.typography.body1Header
            )
            Text(
                text = "Session ID: ${state.dappLink?.sessionId}",
                style = RadixTheme.typography.body1Header
            )
            Text(
                text = "Received Public Key: ${state.receivedPublicKey}",
                style = RadixTheme.typography.body1Header
            )
            state.radixConnectUrl?.let {
                Text(
                    text = "Radix Connect URL: $it",
                    style = RadixTheme.typography.body1Header
                )
            }
        }
        FullscreenCircularProgressContent()
    }
}
