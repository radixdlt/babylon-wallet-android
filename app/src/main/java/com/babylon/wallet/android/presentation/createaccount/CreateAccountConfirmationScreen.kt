package com.babylon.wallet.android.presentation.createaccount

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun CreateAccountConfirmationScreen(
    viewModel: CreateAccountConfirmationViewModel,
    modifier: Modifier = Modifier,
    navigateToWallet: () -> Unit,
    finishAccountCreation: () -> Unit,
) {
    val accountState = viewModel.accountUiState
    SetStatusBarColor(color = RadixTheme.colors.orange2, useDarkIcons = !isSystemInDarkTheme())
    CreateAccountConfirmationContent(
        modifier = modifier,
        accountName = accountState.accountName,
        accountId = accountState.accountAddressTruncated,
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
        modifier = modifier.background(RadixTheme.colors.defaultBackground)
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
        CreatedAccountCardStack(Modifier.fillMaxWidth(0.8f), appearanceId, accountName, accountId)
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.congratulations),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            text = stringResource(id = R.string.your_account_has_been_created),
            style = RadixTheme.typography.body2Link,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.account_created_info),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body2Link,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.weight(0.6f))
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(
                id = when (requestSource) {
                    CreateAccountRequestSource.Wallet -> R.string.go_to_wallet
                    CreateAccountRequestSource.ChooseAccount -> R.string.go_to_choose_accounts
                    CreateAccountRequestSource.FirstTime -> R.string.go_to_home
                    CreateAccountRequestSource.Settings -> R.string.go_to_settings
                }
            ),
            onClick = accountConfirmed
        )
    }
    BackHandler(enabled = true) { }
}

@Composable
private fun CreatedAccountCardStack(
    modifier: Modifier,
    appearanceId: Int,
    accountName: String,
    accountAddress: String,
) {
    val numberOfOtherCards = 4
    val singleCardHeight = 80.dp
    val offset = 6.dp
    Box(modifier = modifier.height(singleCardHeight + offset * numberOfOtherCards)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(singleCardHeight)
                .zIndex(5f)
                .background(
                    Brush.horizontalGradient(AccountGradientList[appearanceId]),
                    RadixTheme.shapes.roundedRectSmall
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = accountName,
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body2Regular,
                    color = Color.White
                )
                Text(
                    text = accountAddress,
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body2Link,
                    color = Color.White
                )
            }
        }
        repeat(4) {
            val index = it + 1
            val nextColors =
                AccountGradientList[(appearanceId + index) % AccountGradientList.size]
                    .map { color -> color.copy(alpha = 0.3f) }
            Box(
                modifier = Modifier
                    .offset(y = offset * index)
                    .fillMaxWidth(1f - 0.05f * (index))
                    .height(singleCardHeight)
                    .zIndex(5f - index)
                    .background(
                        Brush.horizontalGradient(nextColors),
                        RadixTheme.shapes.roundedRectSmall
                    )
                    .align(Alignment.Center)
            )
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
            appearanceId = 0,
            requestSource = CreateAccountRequestSource.FirstTime
        )
    }
}
