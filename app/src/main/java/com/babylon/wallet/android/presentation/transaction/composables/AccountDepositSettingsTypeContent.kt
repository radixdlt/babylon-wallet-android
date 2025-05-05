package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.Image
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
import com.babylon.wallet.android.presentation.model.displayTitleAsToken
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Suppress("CyclomaticComplexMethod")
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
        if (preview.hasSettingSection) {
            SectionHeader(
                Modifier.fillMaxWidth(),
                stringResource(id = R.string.interactionReview_thirdPartyDepositSettingHeading).uppercase()
            )
        }
        preview.accountsWithDepositSettingsChanges.filter { it.defaultDepositRule != null }.forEach { accountWithSettings ->
            CardColumn {
                AccountCardHeader(account = InvolvedAccount.Owned(accountWithSettings.account))
                accountWithSettings.defaultDepositRule?.let { newRule ->
                    val ruleText = stringResource(
                        id = when (newRule) {
                            DepositRule.ACCEPT_ALL -> R.string.interactionReview_depositSettings_acceptAllRule
                            DepositRule.DENY_ALL -> R.string.interactionReview_depositSettings_denyAllRule
                            DepositRule.ACCEPT_KNOWN -> R.string.interactionReview_depositSettings_acceptKnownRule
                        }
                    ).formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold))
                    val icon = when (newRule) {
                        DepositRule.ACCEPT_ALL -> com.babylon.wallet.android.designsystem.R.drawable.ic_accept_all
                        DepositRule.DENY_ALL -> com.babylon.wallet.android.designsystem.R.drawable.ic_deny_all
                        DepositRule.ACCEPT_KNOWN -> com.babylon.wallet.android.designsystem.R.drawable.ic_accept_known
                    }
                    val ruleBackgroundShape =
                        if (accountWithSettings.onlyDepositRuleChanged) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape

                    Row(
                        modifier = Modifier
                            .background(RadixTheme.colors.backgroundSecondary, shape = ruleBackgroundShape)
                            .padding(RadixTheme.dimensions.paddingDefault),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingDefault),
                            painter = painterResource(id = icon),
                            contentDescription = null
                        )
                        Text(
                            text = ruleText,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text
                        )
                    }
                }
            }
        }
        if (preview.hasExceptionsSection) {
            SectionHeader(
                Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingMedium),
                stringResource(id = R.string.interactionReview_thirdPartyDepositExceptionsHeading).uppercase()
            )
            preview.accountsWithDepositSettingsChanges.filter {
                it.depositorChanges.isNotEmpty() || it.assetChanges.isNotEmpty()
            }.forEach { accountWithSettings ->
                CardColumn {
                    AccountCardHeader(account = InvolvedAccount.Owned(accountWithSettings.account))
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
                                assetChange.change.Layout(Modifier.widthIn(max = 90.dp))
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
                                .background(RadixTheme.colors.backgroundSecondary, shape)
                                .padding(RadixTheme.dimensions.paddingDefault),
                            trailingSection = {
                                depositorChange.change.Layout(Modifier.widthIn(max = 90.dp))
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
}

@Composable
private fun CardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(vertical = RadixTheme.dimensions.paddingSmall)
            .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
            .background(
                color = RadixTheme.colors.background,
                shape = RadixTheme.shapes.roundedRectDefault
            )
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .dashedCircleBorder(RadixTheme.colors.gray3) // TODO Theme
                .padding(RadixTheme.dimensions.paddingXXSmall),
            painter = painterResource(
                id = DSR.ic_deposit_changes_heading
            ),
            contentDescription = null,
            tint = RadixTheme.colors.iconSecondary
        )
        Text(
            text = text,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.textSecondary
        )
    }
}

@Composable
private fun PreferenceChange(modifier: Modifier = Modifier, text: String, icon: Int) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )
        Text(
            text = text,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Layout(modifier: Modifier = Modifier) {
    val (text, icon) = when (this) {
        AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add -> {
            stringResource(id = R.string.interactionReview_depositExceptions_depositorChangeAdd) to DSR.ic_add_circle
        }

        AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove -> {
            stringResource(id = R.string.interactionReview_depositExceptions_depositorChangeRemove) to DSR.ic_minus_circle
        }
    }
    PreferenceChange(modifier = modifier, text = text, icon = icon)
}

@Composable
private fun AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Layout(modifier: Modifier = Modifier) {
    val (text, icon) = when (this) {
        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow -> {
            stringResource(id = R.string.interactionReview_depositExceptions_assetChangeAllow) to DSR.ic_accept_all
        }

        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow -> {
            stringResource(id = R.string.interactionReview_depositExceptions_assetChangeDisallow) to DSR.ic_deny_all
        }

        AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear -> {
            stringResource(id = R.string.interactionReview_depositExceptions_assetChangeClear) to DSR.ic_minus_circle
        }
    }
    PreferenceChange(modifier, text = text, icon = icon)
}

@Composable
private fun AccountRuleChangeRow(resource: Resource?, modifier: Modifier = Modifier, trailingSection: @Composable () -> Unit) {
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
                modifier = Modifier.size(44.dp)
            )

            else -> {}
        }
        val name = when (resource) {
            is Resource.FungibleResource -> resource.displayTitleAsToken()
            is Resource.NonFungibleResource -> resource.name
            null -> ""
        }.ifEmpty { stringResource(id = R.string.account_poolUnits_unknownSymbolName) }
        Text(
            text = name,
            style = RadixTheme.typography.body1Regular,
            maxLines = 1,
            modifier = Modifier.weight(0.7f),
            color = RadixTheme.colors.text
        )
        Row(modifier = Modifier.weight(0.3f), horizontalArrangement = Arrangement.Center) {
            trailingSection()
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountDepositSettingsTypeContentPreview() {
    RadixWalletTheme {
        AccountDepositSettingsTypeContent(
            preview = PreviewType.AccountsDepositSettings(
                accountsWithDepositSettingsChanges = listOf(
                    AccountWithDepositSettingsChanges(
                        account = Account.sampleMainnet(),
                        defaultDepositRule = DepositRule.ACCEPT_ALL,
                        assetChanges = listOf(
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow,
                                Resource.NonFungibleResource.sampleMainnet()
                            ),
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow,
                                Resource.NonFungibleResource.sampleMainnet.other()
                            ),
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear,
                                Resource.NonFungibleResource.sampleMainnet.random()
                            )
                        ),
                        depositorChanges = listOf(
                            AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                                Resource.NonFungibleResource.sampleMainnet()
                            ),
                            AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove,
                                Resource.NonFungibleResource.sampleMainnet.other()
                            )
                        )
                    )
                ),
                badges = emptyList()
            )
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustRule() {
    RadixWalletTheme {
        AccountDepositSettingsTypeContent(
            preview = PreviewType.AccountsDepositSettings(
                accountsWithDepositSettingsChanges = listOf(
                    AccountWithDepositSettingsChanges(
                        account = Account.sampleMainnet(),
                        defaultDepositRule = DepositRule.ACCEPT_ALL,
                        assetChanges = emptyList(),
                        depositorChanges = emptyList()
                    )
                ),
                badges = emptyList()
            )
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustAssetChanges() {
    RadixWalletTheme {
        AccountDepositSettingsTypeContent(
            preview = PreviewType.AccountsDepositSettings(
                accountsWithDepositSettingsChanges = listOf(
                    AccountWithDepositSettingsChanges(
                        account = Account.sampleMainnet(),
                        assetChanges = listOf(
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow,
                                Resource.NonFungibleResource.sampleMainnet()
                            ),
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow,
                                Resource.NonFungibleResource.sampleMainnet.other()
                            ),
                            AccountWithDepositSettingsChanges.AssetPreferenceChange(
                                AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear,
                                Resource.NonFungibleResource.sampleMainnet.random()
                            )
                        )
                    )
                ),
                badges = emptyList()
            )
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountDepositSettingsTypeContentPreviewJustDepositorChanges() {
    RadixWalletTheme {
        AccountDepositSettingsTypeContent(
            preview = PreviewType.AccountsDepositSettings(
                accountsWithDepositSettingsChanges = listOf(
                    AccountWithDepositSettingsChanges(
                        account = Account.sampleMainnet(),
                        depositorChanges = listOf(
                            AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                                Resource.NonFungibleResource.sampleMainnet()
                            ),
                            AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                                AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove,
                                Resource.NonFungibleResource.sampleMainnet.other()
                            )
                        )
                    )
                ),
                badges = emptyList()
            )
        )
    }
}
