package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.utils.setSpanForPlaceholder
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
    dappMetadata: DappWithMetadata?,
    isOneTime: Boolean,
    isSingleChoice: Boolean,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    showBackButton: Boolean,
) {
    Box(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = if (showBackButton) Icons.Filled.ArrowBack else Icons.Filled.Clear,
                    contentDescription = "clear"
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    AsyncImage(
                        model = rememberImageUrl(fromUrl = dappMetadata?.iconUrl?.toString(), size = ImageSize.MEDIUM),
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
                        dappName = dappMetadata?.name.orEmpty().ifEmpty { stringResource(id = R.string.unknown_dapp) },
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
                        text = stringResource(id = R.string.create_dapp_accounts_button_title),
                        onClick = onCreateNewAccount
                    )
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
        BottomPrimaryButton(
            onClick = onContinueClick,
            enabled = isContinueButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground)
                .align(Alignment.BottomCenter)
        )
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
            pluralStringResource(
                id = R.plurals.one_time_exactly_request,
                count = numberOfAccounts,
                dappName,
                numberOfAccounts
            )
        } else {
            pluralStringResource(
                id = R.plurals.one_time_at_least_request,
                count = numberOfAccounts,
                dappName,
                numberOfAccounts
            )
        }
    } else {
        if (isExactAccountsCount) {
            pluralStringResource(
                id = R.plurals.ongoing_exactly_request,
                count = numberOfAccounts,
                numberOfAccounts,
                dappName
            )
        } else {
            pluralStringResource(
                id = R.plurals.ongoing_at_least_request,
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
