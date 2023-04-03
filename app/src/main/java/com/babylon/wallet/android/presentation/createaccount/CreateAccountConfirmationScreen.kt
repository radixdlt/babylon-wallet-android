package com.babylon.wallet.android.presentation.createaccount

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.CardStack

@Composable
fun CreateAccountConfirmationScreen(
    viewModel: CreateAccountConfirmationViewModel,
    modifier: Modifier = Modifier,
    navigateToWallet: () -> Unit,
    finishAccountCreation: () -> Unit,
) {
    val accountState by viewModel.state.collectAsState()
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
        CardStack(Modifier.fillMaxWidth(0.8f), appearanceId, accountName, accountId)
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.congratulations),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        val text = if (requestSource == CreateAccountRequestSource.FirstTime) {
            stringResource(id = R.string.you_ve_created_your_first_account)
        } else {
            stringResource(id = R.string.your_account_has_been_created)
        }
        Text(
            text = text,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            text = stringResource(id = R.string.account_created_info),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.weight(0.6f))
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(
                id = when (requestSource) {
                    CreateAccountRequestSource.AccountsList -> R.string.go_to_account_list
                    CreateAccountRequestSource.ChooseAccount -> R.string.go_to_choose_accounts
                    CreateAccountRequestSource.FirstTime -> R.string.go_to_home
                    CreateAccountRequestSource.Gateways -> R.string.go_to_gateways
                }
            ),
            onClick = accountConfirmed
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
