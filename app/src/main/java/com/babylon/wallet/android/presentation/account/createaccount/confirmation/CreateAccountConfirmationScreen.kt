package com.babylon.wallet.android.presentation.account.createaccount.confirmation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.AccountCardWithStack
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AppearanceId

@Composable
fun CreateAccountConfirmationScreen(
    viewModel: CreateAccountConfirmationViewModel,
    modifier: Modifier = Modifier,
    navigateToWallet: () -> Unit,
    finishAccountCreation: () -> Unit,
) {
    val accountState by viewModel.state.collectAsStateWithLifecycle()
    CreateAccountConfirmationContent(
        modifier = modifier,
        accountName = accountState.accountName,
        accountId = accountState.accountAddress,
        accountConfirmed = viewModel::accountConfirmed,
        appearanceId = accountState.appearanceId,
        requestSource = viewModel.args.requestSource
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                CreateAccountConfirmationEvent.NavigateToHome -> navigateToWallet()
                CreateAccountConfirmationEvent.FinishAccountCreation -> finishAccountCreation()
            }
        }
    }
}

@Composable
fun CreateAccountConfirmationContent(
    modifier: Modifier,
    accountName: String,
    accountId: String,
    accountConfirmed: () -> Unit,
    appearanceId: AppearanceId,
    requestSource: CreateAccountRequestSource,
) {
    BackHandler(enabled = true) { }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            RadixBottomBar(
                onClick = accountConfirmed,
                text = stringResource(
                    id = R.string.createEntity_completion_goToDestination,
                    when (requestSource) {
                        CreateAccountRequestSource.AccountsList,
                        CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled,
                        CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled -> stringResource(
                            id = R.string.createEntity_completion_destinationHome
                        )
                        CreateAccountRequestSource.ChooseAccount -> stringResource(
                            R.string.createEntity_completion_destinationChooseAccounts
                        )
                        CreateAccountRequestSource.Gateways -> stringResource(id = R.string.createEntity_completion_destinationGateways)
                    }
                )
            )
        },
        containerColor = RadixTheme.colors.background,
        contentWindowInsets = WindowInsets.statusBarsAndBanner.add(WindowInsets.navigationBars)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(2f))
            AccountCardWithStack(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                appearanceId = appearanceId,
                accountName = accountName,
                accountAddress = accountId
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.createEntity_completion_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            val text = if (requestSource.isFirstTime()) {
                stringResource(id = R.string.createAccount_completion_subtitleFirst)
            } else {
                stringResource(id = R.string.createAccount_completion_subtitleNotFirst)
            }
            Text(
                text = text,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.createEntity_nameNewEntity_explanation),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.weight(3f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountConfirmationContentPreview() {
    RadixWalletTheme {
        CreateAccountConfirmationContent(
            modifier = Modifier,
            accountName = "My Account",
            accountId = "mock_account_id",
            accountConfirmed = {},
            appearanceId = AppearanceId(0u),
            requestSource = CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled
        )
    }
}
