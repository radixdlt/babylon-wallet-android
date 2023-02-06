package com.babylon.wallet.android.presentation.dapp.permission

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.utils.setSpanForPlaceholder

@Composable
fun DAppPermissionScreen(
    viewModel: DAppLoginViewModel,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    numberOfAccounts: Int,
    quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier,
    onCompleteFlow: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppLoginEvent.RejectLogin -> onCompleteFlow()
                else -> {}
            }
        }
    }
    BackHandler(enabled = true) {}
    DAppPermissionContent(
        onContinueClick = {
            viewModel.onPermissionAgree(numberOfAccounts, quantifier)
        },
        dappMetadata = state.dappMetadata,
        showProgress = state.showProgress,
        onRejectClick = viewModel::onRejectLogin,
        numberOfAccounts = numberOfAccounts,
        quantifier = quantifier
    )
}

@Composable
private fun DAppPermissionContent(
    onContinueClick: () -> Unit,
    dappMetadata: DappMetadata?,
    showProgress: Boolean,
    onRejectClick: () -> Unit,
    numberOfAccounts: Int,
    quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        IconButton(onClick = onRejectClick) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "clear"
            )
        }
        AnimatedVisibility(visible = showProgress, modifier = Modifier.fillMaxSize()) {
            FullscreenCircularProgressContent()
        }
        AnimatedVisibility(visible = !showProgress, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AsyncImage(
                    model = dappMetadata?.getIcon(),
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    fallback = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectDefault)
                        .clip(RadixTheme.shapes.roundedRectDefault)
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = stringResource(id = R.string.account_permission),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                PermissionRequestHeader(dappName = dappMetadata?.getName() ?: "Unknown dApp")
                Spacer(modifier = Modifier.weight(0.5f))
                RequestedPermissionsList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingLarge),
                    quantifier = quantifier,
                    numberOfAccounts = numberOfAccounts
                )
                Spacer(modifier = Modifier.weight(0.5f))
                RadixPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinueClick,
                    text = stringResource(id = R.string.continue_button_title)
                )
            }
        }
    }
}

@Composable
private fun RequestedPermissionsList(
    quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier,
    numberOfAccounts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        val text = when (quantifier) {
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly -> {
                pluralStringResource(
                    id = R.plurals.view_x_accounts,
                    numberOfAccounts,
                    numberOfAccounts
                )
            }
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast -> {
                pluralStringResource(
                    id = R.plurals.view_x_or_more_accounts,
                    numberOfAccounts,
                    numberOfAccounts
                )
            }
        }
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
    val spanStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1)
    val always = stringResource(id = R.string.always)
    val text = stringResource(id = R.string.dapp_is_requesting_ongoing_permission, dappName).setSpanForPlaceholder(
        dappName,
        spanStyle
    ).setSpanForPlaceholder(always, spanStyle)
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
fun DAppLoginContentPreview() {
    RadixWalletTheme {
        DAppPermissionContent(
            onContinueClick = {},
            dappMetadata = DappMetadata("address", mapOf(MetadataConstants.KEY_NAME to "Collabo.fi")),
            showProgress = false,
            onRejectClick = {},
            numberOfAccounts = 2,
            quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DAppLoginContentFirstTimePreview() {
    RadixWalletTheme {
        DAppPermissionContent(
            onContinueClick = {},
            dappMetadata = DappMetadata("address", mapOf(MetadataConstants.KEY_NAME to "Collabo.fi")),
            showProgress = false,
            onRejectClick = {},
            numberOfAccounts = 2,
            quantifier = MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast,
            modifier = Modifier.fillMaxSize()
        )
    }
}
