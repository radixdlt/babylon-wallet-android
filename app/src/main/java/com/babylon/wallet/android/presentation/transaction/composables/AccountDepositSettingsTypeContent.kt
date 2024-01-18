package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
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
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.ret.AccountDefaultDepositRule

@Composable
fun AccountDepositSettingsTypeContent(
    preview: PreviewType.AccountsDepositSettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                modifier = Modifier.dashedCircleBorder(RadixTheme.colors.gray3),
                painter = painterResource(
                    id = DSR.ic_deposit_changes_heading
                ),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(id = R.string.transactionReview_accountDepositSettings_subtitle).uppercase(),
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = RadixTheme.colors.gray2
            )
        }
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
                            AccountDefaultDepositRule.ACCEPT -> R.string.transactionReview_accountDepositSettings_acceptAllRule
                            AccountDefaultDepositRule.REJECT -> R.string.transactionReview_accountDepositSettings_denyAllRule
                            AccountDefaultDepositRule.ALLOW_EXISTING -> R.string.transactionReview_accountDepositSettings_acceptKnownRule
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
                    HorizontalDivider(color = RadixTheme.colors.gray4)
                }
                accountWithSettings.assetChanges.forEachIndexed { index, assetChange ->
                    val lastItem = accountWithSettings.assetChanges.lastIndex == index && accountWithSettings.depositorChanges.isEmpty()
                    val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    AccountRuleChangeRow(
                        resource = assetChange.resource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, shape)
                            .padding(RadixTheme.dimensions.paddingDefault),
                        trailingSection = {
                            assetChange.change.Layout()
                        }
                    )
                    if (!lastItem) {
                        HorizontalDivider(color = RadixTheme.colors.gray4)
                    }
                }
                accountWithSettings.depositorChanges.forEachIndexed { index, depositorChange ->
                    val lastItem = accountWithSettings.depositorChanges.lastIndex == index
                    val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    AccountRuleChangeRow(
                        resource = depositorChange.resource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, shape)
                            .padding(RadixTheme.dimensions.paddingDefault),
                        trailingSection = {
                            depositorChange.change.Layout()
                        }
                    )
                    if (!lastItem) {
                        HorizontalDivider(color = RadixTheme.colors.gray4)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceChange(text: String, icon: Int) {
    Icon(painter = painterResource(id = icon), contentDescription = null, tint = RadixTheme.colors.gray1)
    Text(
        text = text,
        style = RadixTheme.typography.body2HighImportance,
        color = RadixTheme.colors.gray1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Layout() {
    val (text, icon) = when (this) {
        AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add -> {
            stringResource(id = R.string.transactionReview_accountDepositSettings_depositorChangeAdd) to DSR.ic_add_circle
        }

        AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove -> {
            stringResource(id = R.string.transactionReview_accountDepositSettings_depositorChangeRemove) to DSR.ic_minus_circle
        }
    }
    PreferenceChange(text = text, icon = icon)
}

@Composable
private fun AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Layout() {
    val (text, icon) = when (this) {
        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow -> {
            stringResource(id = R.string.transactionReview_accountDepositSettings_assetChangeAllow) to DSR.ic_accept_all
        }

        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow -> {
            stringResource(id = R.string.transactionReview_accountDepositSettings_assetChangeDisallow) to DSR.ic_deny_all
        }

        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear -> {
            stringResource(id = R.string.transactionReview_accountDepositSettings_assetChangeClear) to DSR.ic_minus_circle
        }
    }
    PreferenceChange(text = text, icon = icon)
}

@Composable
private fun AccountRuleChangeRow(resource: Resource?, modifier: Modifier = Modifier, trailingSection: @Composable ColumnScope.() -> Unit) {
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
        val name = when (resource) {
            is Resource.FungibleResource -> resource.displayTitle
            is Resource.NonFungibleResource -> resource.name
            null -> ""
        }.ifEmpty { stringResource(id = R.string.account_poolUnits_unknownSymbolName) }
        Text(
            text = name,
            style = RadixTheme.typography.body1Regular,
            maxLines = 1,
            modifier = Modifier.weight(0.7f),
            color = RadixTheme.colors.gray1
        )
        Column(modifier = Modifier.widthIn(max = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            trailingSection()
        }
    }
}

@Preview
@Composable
fun AccountDepositSettingsTypeContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountDepositSettingsTypeContent(
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

@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustDepositorChanges() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountDepositSettingsTypeContent(
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
