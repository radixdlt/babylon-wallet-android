package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.ret.AccountDefaultDepositRule

@Composable
fun AccountDepositSettingsTypeContent(
    state: State,
    preview: PreviewType.AccountsDepositSettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.transactionReview_thirdPartyDeposits_subtitle).uppercase(),
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.gray2
        )
        preview.accountsWithDepositSettingsChanges.forEach { accountWithSettings ->
            Column(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingSmall)
                    .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                    .background(
                        color = Color.White,
                        shape = RadixTheme.shapes.roundedRectDefault
                    )
                    .padding(RadixTheme.dimensions.paddingMedium)
            ) {
                AccountDepositAccountCardHeader(account = accountWithSettings.account)
                accountWithSettings.defaultDepositRule?.let { newRule ->
                    val ruleText = stringResource(
                        id = when (newRule) {
                            AccountDefaultDepositRule.ACCEPT -> R.string.transactionReview_thirdPartyDeposits_acceptAllRule
                            AccountDefaultDepositRule.REJECT -> R.string.transactionReview_thirdPartyDeposits_denyAllRule
                            AccountDefaultDepositRule.ALLOW_EXISTING -> R.string.transactionReview_thirdPartyDeposits_acceptKnownRule
                        }
                    ).formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1))
                    val ruleBackgroundShape =
                        if (accountWithSettings.onlyDepositRuleChanged) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    Text(
                        text = ruleText,
                        style = RadixTheme.typography.body1Regular,
                        modifier = Modifier
                            .background(RadixTheme.colors.gray5, shape = ruleBackgroundShape)
                            .padding(RadixTheme.dimensions.paddingDefault),
                        color = RadixTheme.colors.gray1
                    )
                }
                if (accountWithSettings.defaultDepositRule != null && accountWithSettings.onlyDepositRuleChanged.not()) {
                    Divider(color = RadixTheme.colors.gray4)
                }
                accountWithSettings.assetChanges.forEachIndexed { index, assetChange ->
                    val lastItem = accountWithSettings.assetChanges.lastIndex == index && accountWithSettings.depositorChanges.isEmpty()
                    val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    AccountDepositChangeRow(
                        resource = assetChange.resource,
                        changeText = assetChange.change.description(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, shape)
                            .padding(RadixTheme.dimensions.paddingDefault)
                    )
                    if (!lastItem) {
                        Divider(color = RadixTheme.colors.gray4)
                    }
                }
                accountWithSettings.depositorChanges.forEachIndexed { index, depositorChange ->
                    val lastItem = accountWithSettings.depositorChanges.lastIndex == index
                    val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    AccountDepositChangeRow(
                        resource = depositorChange.resource,
                        changeText = depositorChange.change.description(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, shape)
                            .padding(RadixTheme.dimensions.paddingDefault)
                    )
                    if (!lastItem) {
                        Divider(color = RadixTheme.colors.gray4)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.description(): String {
    return stringResource(
        id = when (this) {
            AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add -> {
                R.string.transactionReview_thirdPartyDeposits_depositorChangeAdd
            }

            AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove -> {
                R.string.transactionReview_thirdPartyDeposits_depositorChangeRemove
            }
        }
    )
}

@Composable
private fun AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.description(): String {
    return stringResource(
        id = when (this) {
            AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow -> {
                R.string.transactionReview_thirdPartyDeposits_assetChangeAllow
            }

            AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow -> {
                R.string.transactionReview_thirdPartyDeposits_assetChangeDisallow
            }

            AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear -> {
                R.string.transactionReview_thirdPartyDeposits_assetChangeClear
            }
        }
    )
}

@Composable
private fun AccountDepositChangeRow(resource: Resource?, changeText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        when (resource) {
            is Resource.FungibleResource -> Thumbnail.Fungible(
                token = resource,
                modifier = Modifier.size(44.dp)
            )

            is Resource.NonFungibleResource -> Thumbnail.NonFungible(
                collection = resource,
                modifier = Modifier.size(44.dp),
                shape = RadixTheme.shapes.roundedRectSmall
            )

            else -> {}
        }
        Text(
            text = resource?.name.orEmpty()
                .ifEmpty { stringResource(id = R.string.account_poolUnits_unknownSymbolName) },
            style = RadixTheme.typography.body1Regular,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            color = RadixTheme.colors.gray1
        )
        Text(
            text = changeText,
            style = RadixTheme.typography.body1HighImportance,
            modifier = Modifier.fillMaxWidth(0.35f),
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End
        )
    }
}

@Preview
@Composable
fun AccountDepositSettingsTypeContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountDepositSettingsTypeContent(
                state = State(
                    request = SampleDataProvider().transactionRequest,
                    isLoading = false,
                    previewType = PreviewType.NonConforming
                ),
                preview = PreviewType.AccountsDepositSettings(
                    accountsWithDepositSettingsChanges = listOf(
                        AccountWithDepositSettingsChanges(
                            SampleDataProvider().sampleAccount(),
                            defaultDepositRule = AccountDefaultDepositRule.ACCEPT,
                            assetChanges = listOf(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow,
                                    nonFungibleResource("res1")
                                ),
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow,
                                    nonFungibleResource("res2")
                                ),
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear,
                                    nonFungibleResource("res3")
                                )
                            ),
                            depositorChanges = listOf(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                    AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                                    nonFungibleResource("res1")
                                ),
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                    AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove,
                                    nonFungibleResource("res2")
                                )
                            )
                        )
                    )

                )
            )
        }
    }
}

@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustRule() {
    RadixWalletTheme {
        AccountDepositSettingsTypeContent(
            state = State(
                request = SampleDataProvider().transactionRequest,
                isLoading = false,
                previewType = PreviewType.NonConforming
            ),
            preview = PreviewType.AccountsDepositSettings(
                accountsWithDepositSettingsChanges = listOf(
                    AccountWithDepositSettingsChanges(
                        SampleDataProvider().sampleAccount(),
                        defaultDepositRule = AccountDefaultDepositRule.ACCEPT,
                        assetChanges = emptyList(),
                        depositorChanges = emptyList()
                    )
                )
            )
        )
    }
}

@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustAssetChanges() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountDepositSettingsTypeContent(
                state = State(
                    request = SampleDataProvider().transactionRequest,
                    isLoading = false,
                    previewType = PreviewType.NonConforming
                ),
                preview = PreviewType.AccountsDepositSettings(
                    accountsWithDepositSettingsChanges = listOf(
                        AccountWithDepositSettingsChanges(
                            SampleDataProvider().sampleAccount(),
                            assetChanges = listOf(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow,
                                    nonFungibleResource("res1")
                                ),
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow,
                                    nonFungibleResource("res2")
                                ),
                                AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                    AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear,
                                    nonFungibleResource("res3")
                                )
                            )
                        )
                    )

                )
            )
        }
    }
}

@Composable
fun AccountDepositSettingsTypeContentPreviewJustDepositorChanges() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountDepositSettingsTypeContent(
                state = State(
                    request = SampleDataProvider().transactionRequest,
                    isLoading = false,
                    previewType = PreviewType.NonConforming
                ),
                preview = PreviewType.AccountsDepositSettings(
                    accountsWithDepositSettingsChanges = listOf(
                        AccountWithDepositSettingsChanges(
                            SampleDataProvider().sampleAccount(),
                            depositorChanges = listOf(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                    AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                                    nonFungibleResource("res1")
                                ),
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                    AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove,
                                    nonFungibleResource("res2")
                                )
                            )
                        )
                    )

                )
            )
        }
    }
}
