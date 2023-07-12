package com.babylon.wallet.android.presentation.createaccount.confirmation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.AccountCardWithStack

@Composable
fun CreateAccountConfirmationScreen(
    viewModel: CreateAccountConfirmationViewModel,
    modifier: Modifier = Modifier,
    navigateToWallet: () -> Unit,
    finishAccountCreation: () -> Unit,
) {
    val accountState by viewModel.state.collectAsStateWithLifecycle()
    SetStatusBarColor(color = RadixTheme.colors.orange2, useDarkIcons = !isSystemInDarkTheme())
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
    appearanceId: Int,
    requestSource: CreateAccountRequestSource,
) {
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        AccountCardWithStack(Modifier.fillMaxWidth(0.8f), appearanceId, accountName, accountId)
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.createEntity_completion_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        val text = if (requestSource == CreateAccountRequestSource.FirstTime) {
            stringResource(id = R.string.createAccount_completion_subtitleFirst)
        } else {
            stringResource(id = R.string.createAccount_completion_subtitleNotFirst)
        }
        Text(
            text = text,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            text = stringResource(id = R.string.createEntity_nameNewEntity_explanation),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.weight(0.6f))
        RadixPrimaryButton(
            text = when (requestSource) {
                CreateAccountRequestSource.AccountsList -> stringResource(
                    id = R.string.createEntity_completion_goToDestination,
                    stringResource(id = R.string.createEntity_completion_destinationHome)
                )
                CreateAccountRequestSource.ChooseAccount -> stringResource(R.string.createEntity_completion_destinationChooseAccounts)
                CreateAccountRequestSource.FirstTime -> stringResource(R.string.createEntity_completion_destinationHome)
                CreateAccountRequestSource.Gateways -> stringResource(R.string.createEntity_completion_destinationGateways)
            },
            onClick = accountConfirmed,
            modifier = Modifier.fillMaxWidth()
        )
    }
    BackHandler(enabled = true) { }
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
            appearanceId = 0,
            requestSource = CreateAccountRequestSource.FirstTime
        )
    }
}
