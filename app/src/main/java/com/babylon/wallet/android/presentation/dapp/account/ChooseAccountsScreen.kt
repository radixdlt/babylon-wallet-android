package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.BottomContinueButton
import com.babylon.wallet.android.utils.setSpanForPlaceholder
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    sharedViewModel: DAppLoginViewModel,
    onBackClick: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppLoginEvent.LoginFlowCompleted -> onLoginFlowComplete()
                else -> {}
            }
        }
    }

    val state = viewModel.state
    val sharedState = sharedViewModel.state
    ChooseAccountContent(
        onBackClick = onBackClick,
        onContinueClick = {
            sharedViewModel.onAccountsSelected(state.selectedAccounts)
        },
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        accountItems = state.availableAccountItems,
        numberOfAccounts = state.numberOfAccounts,
        quantifier = state.quantifier,
        onAccountSelect = viewModel::onAccountSelect,
        onCreateNewAccount = onAccountCreationClick,
        dappMetadata = sharedState.dappMetadata,
        isOneTime = state.oneTimeRequest,
        isSingleChoice = state.isSingleChoice
    )

    if (state.showProgress) {
        FullscreenCircularProgressContent()
    }

    state.error?.let { error ->
        ErrorAlertDialog(
            title = stringResource(id = R.string.dapp_verification_error_title),
            body = error,
            dismissErrorDialog = dismissErrorDialog
        )
    }
}

@Composable
private fun ChooseAccountContent(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    isContinueButtonEnabled: Boolean,
    accountItems: ImmutableList<AccountItemUiModel>,
    onAccountSelect: (Int) -> Unit,
    isSingleChoice: Boolean,
    modifier: Modifier = Modifier,
    onCreateNewAccount: () -> Unit,
    dappMetadata: DappMetadata?,
    isOneTime: Boolean,
    numberOfAccounts: Int,
    quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AsyncImage(
                    model = dappMetadata?.getIcon(),
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    fallback = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectDefault)
                        .clip(RadixTheme.shapes.roundedRectDefault)
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = if (isOneTime) {
                        stringResource(id = R.string.account_request)
                    } else {
                        stringResource(id = R.string.account_permission)
                    },
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                ChooseAccountsSubtitle(
                    dappName = dappMetadata?.getName() ?: "Unknown dApp",
                    isOneTime = isOneTime,
                    numberOfAccounts = numberOfAccounts,
                    quantifier = quantifier
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            itemsIndexed(accountItems) { index, accountItem ->
                val gradientColor = AccountGradientList[accountItem.appearanceID]
                AccountSelectionCard(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(gradientColor),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .clip(RadixTheme.shapes.roundedRectSmall)
                        .clickable {
                            onAccountSelect(index)
                        },
                    accountName = accountItem.displayName.orEmpty(),
                    address = accountItem.address,
                    checked = accountItem.isSelected,
                    isSingleChoice = isSingleChoice,
                    radioButtonClicked = {
                        onAccountSelect(index)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            item {
                RadixTextButton(
                    text = stringResource(id = R.string.create_dapp_accounts_button_title),
                    onClick = onCreateNewAccount
                )
                Spacer(Modifier.height(100.dp))
            }
        }
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "clear"
            )
        }
        BottomContinueButton(
            onLoginClick = onContinueClick,
            loginButtonEnabled = isContinueButtonEnabled,
            modifier = Modifier
                .fillMaxWidth().background(RadixTheme.colors.defaultBackground)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ChooseAccountsSubtitle(
    dappName: String,
    isOneTime: Boolean,
    numberOfAccounts: Int,
    quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
    modifier: Modifier = Modifier
) {
    val text = if (isOneTime) {
        if (quantifier == MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.AtLeast) {
            pluralStringResource(
                id = R.plurals.one_time_at_least_request,
                count = numberOfAccounts,
                dappName,
                numberOfAccounts
            )
        } else {
            pluralStringResource(
                id = R.plurals.one_time_exactly_request,
                count = numberOfAccounts,
                dappName,
                numberOfAccounts
            )
        }
    } else {
        if (quantifier == MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.AtLeast) {
            pluralStringResource(
                id = R.plurals.ongoing_at_least_request,
                count = numberOfAccounts,
                numberOfAccounts,
                dappName
            )
        } else {
            pluralStringResource(
                id = R.plurals.ongoing_exactly_request,
                count = numberOfAccounts,
                numberOfAccounts,
                dappName
            )
        }
    }.setSpanForPlaceholder(dappName, SpanStyle(color = RadixTheme.colors.gray1, fontWeight = FontWeight.SemiBold))
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray2
    )
}

@Composable
private fun ErrorAlertDialog(
    title: String,
    body: String,
    dismissErrorDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = { Text(text = title, color = Color.Black) },
        text = { Text(text = body, color = Color.Black) },
        confirmButton = {
            TextButton(
                onClick = dismissErrorDialog
            ) {
                Text(stringResource(id = R.string.ok), color = Color.Black)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChooseAccountContentPreview() {
    RadixWalletTheme {
        com.babylon.wallet.android.presentation.dapp.unauthorizedaccount.UnauthorizedChooseAccountContent(
            onBackClick = {},
            onContinueClick = {},
            isContinueButtonEnabled = true,
            accountItems = listOf(
                AccountItemUiModel(
                    displayName = "Account name 1",
                    address = "fdj209d9320",
                    appearanceID = 1,
                    isSelected = true
                ),
                AccountItemUiModel(
                    displayName = "Account name 2",
                    address = "342f23f2",
                    appearanceID = 1,
                    isSelected = false
                )
            ),
            onAccountSelect = {},
            isSingleChoice = false
        ) {}
    }
}
