package com.babylon.wallet.android.presentation.mobileconnect

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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSwitch
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun MobileConnectLinkScreen(
    modifier: Modifier = Modifier,
    viewModel: MobileConnectLinkViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is MobileConnectLinkViewModel.Event.OpenUrl -> {
                    context.openUrl(event.url, event.browserName)
                    onBackClick()
                }

                MobileConnectLinkViewModel.Event.Close -> onBackClick()
            }
        }
    }
    MobileConnectLinkContent(
        modifier = modifier.fillMaxSize(),
        state = state,
        onCloseClick = {},
        onMessageShown = viewModel::onMessageShown,
        onLinkWithDapp = viewModel::onLinkWithDapp,
        onAutoConfirmChange = viewModel::onAutoConfirmChange
    )
}

@Composable
fun MobileConnectLinkContent(
    modifier: Modifier = Modifier,
    state: MobileConnectLinkViewModel.State,
    onCloseClick: () -> Unit,
    onMessageShown: () -> Unit,
    onLinkWithDapp: () -> Unit,
    onAutoConfirmChange: (Boolean) -> Unit
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
        },
        topBar = {
            RadixCenteredTopAppBar(
                backIconType = BackIconType.Close,
                title = stringResource(id = R.string.empty),
                onBackClick = onCloseClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                BottomPrimaryButton(
                    modifier = Modifier.navigationBarsPadding(),
                    text = stringResource(id = R.string.createAccount_nameNewAccount_continue),
                    onClick = onLinkWithDapp,
                    isLoading = state.isLinking,
                    enabled = !state.isLinking
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                text = stringResource(id = R.string.mobileConnect_verifyingDApp_title, dAppDisplayName),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.title, // TODO FIX THIS
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = buildAnnotatedString {
                    val valueToDisplay = stringResource(
                        id = R.string.mobileConnect_verifyingDApp_subtitle,
                        dAppDisplayName
                    )

                    append(valueToDisplay)

                    val startOfSpan = valueToDisplay.indexOf(dAppDisplayName)
                    addStyle(
                        style = RadixTheme.typography.body1StandaloneLink.toSpanStyle(), // TODO FIX THIS
                        start = startOfSpan,
                        end = startOfSpan + dAppDisplayName.length,
                    )
                },
                color = RadixTheme.colors.gray2,
                style = RadixTheme.typography.body1HighImportance, // TODO FIX THIS
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(id = R.string.mobileConnect_verifyingDApp_body),
                color = RadixTheme.colors.gray1,
                style = RadixTheme.typography.body1Regular, // TODO FIX THIS
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.mobileConnect_verifyingDApp_autoConfirmTitle),
                        style = RadixTheme.typography.body1Header, // TODO FIX THIS
                        color = RadixTheme.colors.gray1
                    )
                    Text(
                        text = stringResource(id = R.string.mobileConnect_verifyingDApp_autoConfirmSubtitle),
                        style = RadixTheme.typography.body1Regular, // TODO FIX THIS
                        color = RadixTheme.colors.gray1
                    )
                }
                RadixSwitch(checked = state.autoLink, onCheckedChange = onAutoConfirmChange)
            }
        }
    }
}

@UsesSampleValues
@Composable
@Preview
fun MobileConnectScreenPreview() {
    RadixWalletPreviewTheme {
        MobileConnectLinkContent(
            state = MobileConnectLinkViewModel.State(),
            onCloseClick = {},
            onMessageShown = {},
            onLinkWithDapp = {},
            onAutoConfirmChange = {}
        )
    }
}
