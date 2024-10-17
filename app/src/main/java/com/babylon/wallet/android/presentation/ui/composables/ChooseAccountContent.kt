package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.DApp

@Composable
fun ChooseAccountContent(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    isContinueButtonEnabled: Boolean,
    accountItems: ImmutableList<AccountItemUiModel>,
    onAccountSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCreateNewAccount: () -> Unit,
    dapp: DApp?,
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
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                enabled = isContinueButtonEnabled,
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
                Thumbnail.DApp(
                    modifier = Modifier.size(104.dp),
                    dapp = dapp
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
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
                ChooseAccountsSubtitle(
                    dappName = dapp?.name.orEmpty()
                        .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) },
                    isOneTime = isOneTime,
                    numberOfAccounts = numberOfAccounts,
                    isExactAccountsCount = isExactAccountsCount
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
            itemsIndexed(accountItems) { index, accountItem ->
                AccountSelectionCard(
                    modifier = Modifier
                        .background(
                            accountItem.appearanceID.gradient(),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .clip(RadixTheme.shapes.roundedRectSmall)
                        .clickable {
                            onAccountSelected(index)
                        },
                    accountName = accountItem.displayName.orEmpty(),
                    address = accountItem.address,
                    checked = accountItem.isSelected,
                    isSingleChoice = isSingleChoice,
                    radioButtonClicked = {
                        onAccountSelected(index)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            item {
                RadixSecondaryButton(
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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ChooseAccountContentPreview() {
    RadixWalletPreviewTheme {
        ChooseAccountContent(
            onBackClick = {},
            onContinueClick = {},
            isContinueButtonEnabled = true,
            accountItems = persistentListOf(
                AccountItemUiModel(
                    displayName = "Account name 1",
                    address = AccountAddress.sampleMainnet.random(),
                    appearanceID = AppearanceId(1u),
                    isSelected = true
                ),
                AccountItemUiModel(
                    displayName = "Account name 2",
                    address = AccountAddress.sampleMainnet.random(),
                    appearanceID = AppearanceId(2u),
                    isSelected = false
                )
            ),
            onAccountSelected = {},
            onCreateNewAccount = {},
            dapp = DApp.sampleMainnet(),
            isOneTime = false,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            showBackButton = true
        )
    }
}
