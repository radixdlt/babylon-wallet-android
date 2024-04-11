package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.mobileconnect.State.RequestType.CONNECT
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.utils.openUrl

@Composable
fun MobileConnectScreen(
    modifier: Modifier = Modifier,
    viewModel: MobileConnectViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is MobileConnectViewModel.Event.OpenUrl -> {
                    context.openUrl(event.url)
                    onBackClick()
                }

                MobileConnectViewModel.Event.Close -> onBackClick()
            }
        }
    }
    MobileConnectContent(modifier = modifier.fillMaxSize(), state = state, onMessageShown = viewModel::onMessageShown)
    if (!state.isProfileInitialized) {
        BasicPromptAlertDialog(
            finish = {
                onBackClick()
            },
            titleText = "No profile found",
            messageText = "You need to create a profile to respond to dApp requests",
            confirmText = stringResource(id = R.string.common_ok),
            dismissText = null
        )
    }
}

@Composable
fun MobileConnectContent(
    modifier: Modifier,
    state: State,
    onMessageShown: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.defaultBackground,
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.requestType) {
                CONNECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                    ) {
                        Text(text = "Connecting...", color = RadixTheme.colors.gray1, style = RadixTheme.typography.body1HighImportance)
                        state.dApp?.let { dApp ->
                            Thumbnail.DApp(
                                modifier = Modifier
                                    .size(64.dp),
                                dapp = dApp,
                                shape = RadixTheme.shapes.roundedRectSmall
                            )
                            dApp.name?.let {
                                Text(
                                    text = it, color = RadixTheme.colors.gray1, style = RadixTheme.typography.body1HighImportance
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
            FullscreenCircularProgressContent()
        }
    }
}
