package com.babylon.wallet.android.presentation.wallet

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.FiatPrice
import com.babylon.wallet.android.domain.model.assets.SupportedCurrency
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.toText

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
fun AccountCardView(
    modifier: Modifier = Modifier,
    accountWithAssets: AccountWithAssets,
    fiatTotalValue: FiatPrice?,
    accountTag: WalletUiState.AccountTag?,
    isLoadingResources: Boolean,
    isLoadingBalance: Boolean,
    securityPromptType: SecurityPromptType?,
    onApplySecuritySettings: (SecurityPromptType) -> Unit
) {
    val gradient = remember(accountWithAssets.account.appearanceID) {
        AccountGradientList[accountWithAssets.account.appearanceID % AccountGradientList.size]
    }
    ConstraintLayout(
        modifier
            .background(Brush.linearGradient(gradient), shape = RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .padding(
                vertical = RadixTheme.dimensions.paddingDefault,
                horizontal = RadixTheme.dimensions.paddingLarge
            )
    ) {
        val (nameLabel, fiatTotalValueLabel, legacyLabel, addressLabel, spacer, assetsContainer, promptsContainer) = createRefs()

        Text(
            modifier = Modifier.constrainAs(nameLabel) {
                linkTo(start = parent.start, end = fiatTotalValueLabel.start, bias = 0f)
                width = Dimension.fillToConstraints
            },
            text = accountWithAssets.account.displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
        )

        TotalFiatBalanceView(
            modifier = Modifier.constrainAs(fiatTotalValueLabel) {
                start.linkTo(nameLabel.end, margin = 10.dp)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
            },
            fiatPrice = fiatTotalValue,
            currency = SupportedCurrency.USD,
            isLoading = isLoadingBalance,
            contentColor = RadixTheme.colors.white,
            hiddenContentColor = RadixTheme.colors.white.copy(alpha = 0.6f),
            contentStyle = RadixTheme.typography.body1Header
        )

        ActionableAddressView(
            address = accountWithAssets.account.address,
            modifier = Modifier.constrainAs(addressLabel) {
                top.linkTo(nameLabel.bottom, margin = 8.dp)
                start.linkTo(parent.start)
            },
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
        )

        accountTag?.let {
            val context = LocalContext.current
            val tagLabel = remember(accountTag) {
                accountTag.toLabel(context)
            }
            Text(
                modifier = Modifier.constrainAs(legacyLabel) {
                    start.linkTo(addressLabel.end, margin = 8.dp)
                    bottom.linkTo(addressLabel.bottom)
                },
                text = tagLabel,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.white
            )
        }

        Spacer(
            modifier = Modifier.constrainAs(spacer) {
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    top = addressLabel.bottom,
                    bottom = assetsContainer.top,
                )
                height = Dimension.value(32.dp)
            }
        )

        AccountAssetsRow(
            modifier = Modifier.constrainAs(assetsContainer) {
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    top = spacer.bottom,
                    bottom = if (securityPromptType != null) promptsContainer.top else parent.bottom,
                    bottomMargin = if (securityPromptType != null) 18.dp else 0.dp
                )
                width = Dimension.fillToConstraints
            },
            assets = accountWithAssets.assets,
            isLoading = isLoadingResources
        )

        Column(
            modifier = Modifier.constrainAs(promptsContainer) {
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    top = assetsContainer.bottom,
                    bottom = parent.bottom,
                    verticalBias = 1f
                )
            }
        ) {
            securityPromptType?.let {
                ApplySecuritySettingsLabel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onApplySecuritySettings(it)
                    },
                    text = securityPromptType.toText()
                )
            }
        }
    }
}

private fun WalletUiState.AccountTag.toLabel(context: Context): String {
    return when (this) {
        WalletUiState.AccountTag.LEDGER_BABYLON -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_ledgerBabylon))
                .toString()
        }
        WalletUiState.AccountTag.LEDGER_LEGACY -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_ledgerLegacy))
                .toString()
        }
        WalletUiState.AccountTag.LEGACY_SOFTWARE -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_legacySoftware))
                .toString()
        }
        WalletUiState.AccountTag.DAPP_DEFINITION -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_dAppDefinition))
                .toString()
        }
    }
}

@Preview
@Composable
fun AccountCardPreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountWithAssets(
                    account = SampleDataProvider().sampleAccount(),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    )
                ),
                fiatTotalValue = FiatPrice(price = 3450900.899, currency = SupportedCurrency.USD),
                accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                isLoadingResources = false,
                isLoadingBalance = false,
                securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                onApplySecuritySettings = {}
            )
        }
    }
}

@Preview
@Composable
fun AccountCardWithLongNameAndShortTotalValuePreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountWithAssets(
                    account = SampleDataProvider().sampleAccount(
                        name = "a very long name for my account"
                    ),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    )
                ),
                fiatTotalValue = FiatPrice(price = 3450.0, currency = SupportedCurrency.USD),
                accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                isLoadingResources = false,
                isLoadingBalance = false,
                securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                onApplySecuritySettings = {}
            )
        }
    }
}

@Preview
@Composable
fun AccountCardWithLongNameAndLongTotalValuePreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountWithAssets(
                    account = SampleDataProvider().sampleAccount(
                        name = "a very long name for my account again much more longer oh god "
                    ),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    )
                ),
                fiatTotalValue = FiatPrice(price = 3450900899900899732.4, currency = SupportedCurrency.USD),
                accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                isLoadingResources = false,
                isLoadingBalance = false,
                securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                onApplySecuritySettings = {}
            )
        }
    }
}

@Preview
@Composable
fun AccountCardWithLongNameAndTotalValueHiddenPreview() {
    RadixWalletPreviewTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AccountCardView(
                    accountWithAssets = AccountWithAssets(
                        account = SampleDataProvider().sampleAccount(
                            name = "a very long name for my account again much more longer oh god "
                        ),
                        assets = Assets(
                            tokens = emptyList(),
                            nonFungibles = listOf(),
                            poolUnits = emptyList(),
                            liquidStakeUnits = emptyList(),
                            stakeClaims = emptyList()
                        )
                    ),
                    fiatTotalValue = FiatPrice(price = 3450900899900899732.4, currency = SupportedCurrency.USD),
                    accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                    isLoadingResources = false,
                    isLoadingBalance = false,
                    securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                    onApplySecuritySettings = {}
                )
            }
        }
    }
}

@Preview
@Composable
fun AccountCardLoadingPreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountWithAssets(
                    account = SampleDataProvider().sampleAccount(),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    )
                ),
                fiatTotalValue = FiatPrice(price = 3450900899.0, currency = SupportedCurrency.USD),
                accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                isLoadingResources = true,
                isLoadingBalance = true,
                securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                onApplySecuritySettings = {}
            )
        }
    }
}
