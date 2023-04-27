package com.babylon.wallet.android.presentation.dapp.authorized.permission

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.utils.setSpanForPlaceholder

@Composable
fun LoginPermissionScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onChooseAccounts: (DAppAuthorizedLoginEvent.ChooseAccounts) -> Unit,
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
                is DAppAuthorizedLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppAuthorizedLoginEvent.RejectLogin -> onCompleteFlow()
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
        dappMetadata = state.dappMetadata,
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
    dappMetadata: DappMetadata?,
    onBackClick: () -> Unit,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    modifier: Modifier = Modifier,
    isFirstScreenInFlow: Boolean,
) {
    Column(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = if (isFirstScreenInFlow) Icons.Filled.Clear else Icons.Filled.ArrowBack,
                contentDescription = "clear"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            AsyncImage(
                model = rememberImageUrl(fromUrl = dappMetadata?.iconUrl?.toString(), size = ImageSize.MEDIUM),
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
            PermissionRequestHeader(dappName = dappMetadata?.name.orEmpty().ifEmpty { stringResource(id = R.string.unknown_dapp) })
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
                text = stringResource(R.string.you_can_update_permission_at_any_time),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
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
        val text = if (isExactAccountsCount) {
            pluralStringResource(
                id = R.plurals.view_exactly_x_accounts,
                numberOfAccounts,
                numberOfAccounts
            )
        } else {
            if (numberOfAccounts == 0) {
                stringResource(id = R.string.any_number_of_accounts)
            } else {
                pluralStringResource(
                    id = R.plurals.view_at_least_x_accounts,
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
fun LoginPermissionContentPreview() {
    RadixWalletTheme {
        LoginPermissionContent(
            onContinueClick = {},
            dappMetadata = DappMetadata(
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
            dappMetadata = DappMetadata(
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
