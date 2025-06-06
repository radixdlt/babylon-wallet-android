package com.babylon.wallet.android.presentation.dapp.authorized.ongoingaccounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.DApp

@Composable
fun OngoingAccountsScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    onCompleteFlow: () -> Unit,
    onBackClick: () -> Unit,
    isOneTimeRequest: Boolean,
    showBack: Boolean
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler {
        if (state.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingAccounts) {
            viewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.NavigateToChooseAccounts -> onChooseAccounts(event)
                is Event.CloseLoginFlow -> onCompleteFlow()
                else -> {}
            }
        }
    }

    OngoingAccountsContent(
        onContinueClick = {
            viewModel.onAccountPermissionGranted(
                isOneTimeRequest = isOneTimeRequest,
                isExactAccountsCount = isExactAccountsCount,
                numberOfAccounts = numberOfAccounts
            )
        },
        dapp = state.dapp,
        onBackClick = {
            if (state.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingAccounts) {
                viewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        numberOfAccounts = numberOfAccounts,
        isExactAccountsCount = isExactAccountsCount,
        showBack = showBack
    )
}

@Composable
private fun OngoingAccountsContent(
    onContinueClick: () -> Unit,
    dapp: DApp?,
    onBackClick: () -> Unit,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    modifier: Modifier = Modifier,
    showBack: Boolean
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (showBack) BackIconType.Back else BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = R.string.dAppRequest_accountPermission_continue)
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + RadixTheme.dimensions.paddingXXLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Thumbnail.DApp(
                modifier = Modifier.size(64.dp),
                dapp = dapp,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.dAppRequest_accountPermission_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            PermissionRequestHeader(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                dappName = dapp.displayName()
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            RequestedPermissionsList(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge)
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.backgroundSecondary,
                        shape = RadixTheme.shapes.roundedRectMedium
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingLarge
                    ),
                isExactAccountsCount = isExactAccountsCount,
                numberOfAccounts = numberOfAccounts
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = stringResource(R.string.dAppRequest_accountPermission_updateInSettingsExplanation),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RequestedPermissionsList(
    isExactAccountsCount: Boolean,
    numberOfAccounts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val text = StringBuilder(stringResource(id = R.string.dot_separator)).apply {
            append(" ")
            if (isExactAccountsCount) {
                if (numberOfAccounts > 1) {
                    append(stringResource(id = R.string.dAppRequest_accountPermission_numberOfAccountsExactly, numberOfAccounts))
                } else {
                    append(stringResource(id = R.string.dAppRequest_accountPermission_numberOfAccountsExactlyOne))
                }
            } else {
                if (numberOfAccounts == 0) {
                    append(stringResource(id = R.string.dAppRequest_accountPermission_numberOfAccountsAtLeastZero))
                } else {
                    append(stringResource(id = R.string.dAppRequest_accountPermission_numberOfAccountsAtLeast, numberOfAccounts))
                }
            }
        }.toString()
        Text(
            text = text,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
    }
}

@Composable
private fun PermissionRequestHeader(
    dappName: String,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_accountPermission_subtitle, dappName)
        .formattedSpans(
            boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.text)
        )
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.textSecondary
    )
}

@UsesSampleValues
@Preview
@Composable
fun OngoingAccountsContentPreviewLight() {
    RadixWalletPreviewTheme {
        OngoingAccountsContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            showBack = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun OngoingAccountsContentPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        OngoingAccountsContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            showBack = true
        )
    }
}

@UsesSampleValues
@Preview(device = "id:Nexus S")
@Composable
fun OngoingAccountsContentSmallDevicePreview() {
    RadixWalletPreviewTheme {
        OngoingAccountsContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            showBack = true
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun OngoingAccountsContentFirstTimePreviewLight() {
    RadixWalletPreviewTheme {
        OngoingAccountsContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            showBack = false
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun OngoingAccountsContentFirstTimePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        OngoingAccountsContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            showBack = false
        )
    }
}
