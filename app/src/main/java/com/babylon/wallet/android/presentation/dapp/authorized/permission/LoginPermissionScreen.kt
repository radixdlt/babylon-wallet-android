package com.babylon.wallet.android.presentation.dapp.authorized.permission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun LoginPermissionScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    onCompleteFlow: () -> Unit,
    onBackClick: () -> Unit,
    oneTime: Boolean
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.CloseLoginFlow -> onCompleteFlow()
                else -> {}
            }
        }
    }
    BackHandler {
        if (state.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission) {
            viewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }
    LoginPermissionContent(
        onContinueClick = {
            viewModel.onPermissionGranted(numberOfAccounts, isExactAccountsCount, oneTime)
        },
        dappWithMetadata = state.dappWithMetadata,
        onBackClick = {
            if (state.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission) {
                viewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        numberOfAccounts = numberOfAccounts,
        isExactAccountsCount = isExactAccountsCount,
        isFirstScreenInFlow = state.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission
    )
}

@Composable
private fun LoginPermissionContent(
    onContinueClick: () -> Unit,
    dappWithMetadata: DAppWithMetadata?,
    onBackClick: () -> Unit,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    modifier: Modifier = Modifier,
    isFirstScreenInFlow: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (isFirstScreenInFlow) BackIconType.Close else BackIconType.Back,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(RadixTheme.dimensions.paddingDefault)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Thumbnail.DApp(
                modifier = Modifier
                    .size(64.dp),
                dapp = dappWithMetadata,
                shape = RadixTheme.shapes.roundedRectSmall
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.dAppRequest_accountPermission_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            PermissionRequestHeader(
                dappName = dappWithMetadata?.name.orEmpty()
                    .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) }
            )
            Spacer(modifier = Modifier.weight(0.5f))
            RequestedPermissionsList(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectMedium)
                    .padding(RadixTheme.dimensions.paddingLarge),
                isExactAccountsCount = isExactAccountsCount,
                numberOfAccounts = numberOfAccounts
            )
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.dAppRequest_accountPermission_updateInSettingsExplanation),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.weight(0.5f))
            RadixPrimaryButton(
                text = stringResource(id = R.string.dAppRequest_accountPermission_continue),
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth()
            )
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
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
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun PermissionRequestHeader(
    dappName: String,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_accountPermission_subtitle, dappName)
        .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1))
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray2
    )
}

@Preview(showBackground = true)
@Composable
fun LoginPermissionContentPreview() {
    RadixWalletTheme {
        LoginPermissionContent(
            onContinueClick = {},
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            isFirstScreenInFlow = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPermissionContentFirstTimePreview() {
    RadixWalletTheme {
        LoginPermissionContent(
            onContinueClick = {},
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            onBackClick = {},
            numberOfAccounts = 2,
            isExactAccountsCount = false,
            modifier = Modifier.fillMaxSize(),
            isFirstScreenInFlow = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RequestedPermissionsListPreview() {
    RadixWalletTheme {
        RequestedPermissionsList(
            numberOfAccounts = 2,
            isExactAccountsCount = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}
