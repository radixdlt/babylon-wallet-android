package com.babylon.wallet.android.presentation.mobileconnect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.utils.formattedSpans
import com.google.accompanist.placeholder.placeholder
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun MobileConnectLinkScreen(
    modifier: Modifier = Modifier,
    viewModel: MobileConnectLinkViewModel = hiltViewModel(),
    onClose: () -> Unit,
    onHandleRequestAuthorizedRequest: (String) -> Unit,
    onHandleUnauthorizedRequest: (String) -> Unit,
    onHandleTransactionRequest: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                MobileConnectLinkViewModel.Event.Close -> onClose()
                is MobileConnectLinkViewModel.Event.HandleRequest -> {
                    when (event.request) {
                        is IncomingMessage.IncomingRequest.AuthorizedRequest -> {
                            onHandleRequestAuthorizedRequest(event.request.interactionId)
                        }

                        is IncomingMessage.IncomingRequest.TransactionRequest -> {
                            onHandleTransactionRequest(event.request.interactionId)
                        }

                        is IncomingMessage.IncomingRequest.UnauthorizedRequest -> {
                            onHandleUnauthorizedRequest(event.request.interactionId)
                        }
                    }
                }
            }
        }
    }
    MobileConnectLinkContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onVerify = viewModel::onVerifyOrigin,
        onDeny = viewModel::onDenyOrigin
    )
}

@Composable
fun MobileConnectLinkContent(
    modifier: Modifier = Modifier,
    state: MobileConnectLinkViewModel.State,
    onMessageShown: () -> Unit,
    onVerify: () -> Unit,
    onDeny: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    BackHandler(onBack = onDeny)

    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.defaultBackground,
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        topBar = {
            RadixCenteredTopAppBar(
                backIconType = BackIconType.Close,
                title = stringResource(id = R.string.empty),
                onBackClick = onDeny,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                BottomPrimaryButton(
                    modifier = Modifier.navigationBarsPadding(),
                    text = stringResource(id = R.string.createAccount_nameNewAccount_continue),
                    onClick = onVerify,
                    isLoading = state.isVerifying,
                    enabled = !state.isVerifying
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(state = rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
            Thumbnail.DApp(
                modifier = Modifier.size(64.dp),
                dapp = state.dApp,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            val dAppDisplayName = state.dApp.displayName()
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(id = R.string.mobileConnect_linkTitle),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge)
                    .placeholder(
                        visible = state.isLoading,
                        color = RadixTheme.colors.gray4,
                        shape = RadixTheme.shapes.roundedRectSmall,
                    ),
                text = stringResource(
                    id = R.string.mobileConnect_linkSubtitle,
                    dAppDisplayName
                ).formattedSpans(RadixTheme.typography.body2Header.copy(fontSize = 16.sp).toSpanStyle()),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Link,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge)
                    .background(color = RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                    .padding(vertical = RadixTheme.dimensions.paddingLarge, horizontal = RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                NumberedListItem(number = 1, text = stringResource(id = R.string.mobileConnect_linkBody1))
                HorizontalDivider(color = RadixTheme.colors.gray4)
                NumberedListItem(number = 2, text = stringResource(id = R.string.mobileConnect_linkBody2))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumberedListItem(modifier: Modifier = Modifier, number: Int, text: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        val color = RadixTheme.colors.gray1
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, color, RadixTheme.shapes.circle)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = number.toString(),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Header.copy(fontSize = 20.sp),
                textAlign = TextAlign.Start
            )
        }
        Text(
            text = text,
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular,
            textAlign = TextAlign.Start
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun MobileConnectScreenPreview() {
    RadixWalletPreviewTheme {
        MobileConnectLinkContent(
            state = MobileConnectLinkViewModel.State(),
            onMessageShown = {},
            onVerify = {},
            onDeny = {}
        )
    }
}
