package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChooseAccountContent(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    isContinueButtonEnabled: Boolean,
    accountItems: ImmutableList<AccountItemUiModel>,
    onAccountSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCreateNewAccount: () -> Unit,
    dappWithMetadata: DAppWithMetadata?,
    isOneTime: Boolean,
    isSingleChoice: Boolean,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    showBackButton: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (showBackButton) BackIconType.Back else BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            BottomPrimaryButton(
                onClick = onContinueClick,
                enabled = isContinueButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                text = stringResource(id = R.string.dAppRequest_chooseAccounts_continue)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        LazyColumn(
            contentPadding = padding + PaddingValues(RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AsyncImage(
                    model = rememberImageUrl(fromUrl = dappWithMetadata?.iconUrl, size = ImageSize.MEDIUM),
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
                        stringResource(id = R.string.dAppRequest_chooseAccountsOneTime_title)
                    } else {
                        stringResource(id = R.string.dAppRequest_chooseAccountsOngoing_title)
                    },
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                ChooseAccountsSubtitle(
                    dappName = dappWithMetadata?.name.orEmpty()
                        .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) },
                    isOneTime = isOneTime,
                    numberOfAccounts = numberOfAccounts,
                    isExactAccountsCount = isExactAccountsCount
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            itemsIndexed(accountItems) { index, accountItem ->
                val gradientColor = AccountGradientList[accountItem.appearanceID % AccountGradientList.size]
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
                    text = stringResource(id = R.string.dAppRequest_chooseAccounts_createNewAccount),
                    onClick = onCreateNewAccount
                )
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ChooseAccountsSubtitle(
    dappName: String,
    isOneTime: Boolean,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (isOneTime) {
        if (isExactAccountsCount) {
            if (numberOfAccounts > 1) {
                stringResource(id = R.string.dAppRequest_chooseAccountsOneTime_subtitleExactly, dappName, numberOfAccounts)
            } else {
                stringResource(id = R.string.dAppRequest_chooseAccountsOneTime_subtitleExactlyOne, dappName)
            }
        } else {
            if (numberOfAccounts > 1) {
                stringResource(id = R.string.dAppRequest_chooseAccountsOneTime_subtitleAtLeast, dappName, numberOfAccounts)
            } else {
                stringResource(id = R.string.dAppRequest_chooseAccountsOneTime_subtitleAtLeastOne, dappName)
            }
        }
    } else {
        if (isExactAccountsCount) {
            if (numberOfAccounts > 1) {
                stringResource(id = R.string.dAppRequest_chooseAccountsOngoing_subtitleExactly, numberOfAccounts, dappName)
            } else {
                stringResource(id = R.string.dAppRequest_chooseAccountsOngoing_subtitleExactlyOne, dappName)
            }
        } else {
            if (numberOfAccounts > 1) {
                stringResource(id = R.string.dAppRequest_chooseAccountsOngoing_subtitleAtLeast, numberOfAccounts, dappName)
            } else {
                stringResource(id = R.string.dAppRequest_chooseAccountsOngoing_subtitleAtLeastOne, dappName)
            }
        }
    }.formattedSpans(boldStyle = SpanStyle(color = RadixTheme.colors.gray1, fontWeight = FontWeight.SemiBold))
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray2
    )
}
