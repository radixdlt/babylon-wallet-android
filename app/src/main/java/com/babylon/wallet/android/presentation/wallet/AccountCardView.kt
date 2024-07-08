package com.babylon.wallet.android.presentation.wallet

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.toText
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.wallet.WalletViewModel.State.AccountTag
import com.babylon.wallet.android.presentation.wallet.WalletViewModel.State.AccountUiItem
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
fun AccountCardView(
    modifier: Modifier = Modifier,
    accountWithAssets: AccountUiItem,
    onApplySecuritySettingsClick: () -> Unit
) {
    ConstraintLayout(
        modifier
            .background(
                brush = accountWithAssets.account.appearanceId.gradient(),
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .padding(
                vertical = RadixTheme.dimensions.paddingDefault,
                horizontal = RadixTheme.dimensions.paddingLarge
            )
    ) {
        val (
            nameLabel,
            fiatTotalValueLabel,
            fiatTotalLoading,
            legacyLabel,
            addressLabel,
            spacer,
            assetsContainer,
            promptsContainer
        ) = createRefs()

        Text(
            modifier = Modifier.constrainAs(nameLabel) {
                linkTo(
                    start = parent.start,
                    end = if (accountWithAssets.isFiatBalanceVisible) {
                        if (accountWithAssets.isLoadingBalance) {
                            fiatTotalLoading.start
                        } else {
                            fiatTotalValueLabel.start
                        }
                    } else {
                        parent.end
                    },
                    bias = 0f
                )
                width = Dimension.fillToConstraints
            },
            text = accountWithAssets.account.displayName.value,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
        )

        if (accountWithAssets.isFiatBalanceVisible) {
            if (accountWithAssets.isLoadingBalance) {
                Row(
                    modifier = Modifier.constrainAs(fiatTotalLoading) {
                        start.linkTo(nameLabel.end, margin = 10.dp)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .fillMaxWidth(0.4f)
                            .radixPlaceholder(
                                visible = true,
                                color = RadixTheme.colors.defaultBackground.copy(alpha = 0.6f),
                                shape = RadixTheme.shapes.roundedRectSmall,
                            ),
                    )
                }
            } else {
                TotalFiatBalanceView(
                    modifier = Modifier.constrainAs(fiatTotalValueLabel) {
                        start.linkTo(nameLabel.end, margin = 10.dp)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    },
                    fiatPrice = accountWithAssets.fiatTotalValue,
                    currency = SupportedCurrency.USD,
                    isLoading = false,
                    contentColor = RadixTheme.colors.white,
                    hiddenContentColor = RadixTheme.colors.white.copy(alpha = 0.6f),
                    onVisibilityToggle = {},
                    contentStyle = RadixTheme.typography.body1Header
                )
            }
        }

        val addressTextColor = RadixTheme.colors.white.copy(alpha = 0.8f)

        ActionableAddressView(
            modifier = Modifier.constrainAs(addressLabel) {
                top.linkTo(nameLabel.bottom, margin = 8.dp)
                start.linkTo(parent.start)
            },
            address = accountWithAssets.account.address.asGeneral(),
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = addressTextColor,
            iconColor = addressTextColor
        )

        accountWithAssets.tag?.let {
            val context = LocalContext.current
            val tagLabel = remember(it) {
                it.toLabel(context)
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
                    bottom = if (accountWithAssets.securityPrompts != null) promptsContainer.top else parent.bottom,
                    bottomMargin = if (accountWithAssets.securityPrompts != null) 18.dp else 0.dp
                )
                width = Dimension.fillToConstraints
            },
            assets = accountWithAssets.assets,
            isLoading = accountWithAssets.isLoadingAssets
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
            accountWithAssets.securityPrompts?.forEach { securityPromptType ->
                ApplySecuritySettingsLabel(
                    modifier = Modifier.fillMaxWidth().padding(bottom = RadixTheme.dimensions.paddingMedium),
                    onClick = onApplySecuritySettingsClick,
                    text = securityPromptType.toText()
                )
            }
        }
    }
}

private fun AccountTag.toLabel(context: Context): String {
    return when (this) {
        AccountTag.LEDGER_BABYLON -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_ledgerBabylon))
                .toString()
        }

        AccountTag.LEDGER_LEGACY -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_ledgerLegacy))
                .toString()
        }

        AccountTag.LEGACY_SOFTWARE -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_legacySoftware))
                .toString()
        }

        AccountTag.DAPP_DEFINITION -> {
            StringBuilder()
                .append(" ")
                .append(context.resources.getString(R.string.dot_separator))
                .append("   ")
                .append(context.resources.getString(R.string.homePage_accountsTag_dAppDefinition))
                .toString()
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountCardPreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountUiItem(
                    account = Account.sampleMainnet(),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    ),
                    fiatTotalValue = FiatPrice(price = 3450900.899.toDecimal192(), currency = SupportedCurrency.USD),
                    tag = AccountTag.DAPP_DEFINITION,
                    securityPrompts = null,
                    isFiatBalanceVisible = true,
                    isLoadingAssets = false,
                    isLoadingBalance = false,
                ),
                onApplySecuritySettingsClick = {}
            )
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountCardWithLongNameAndShortTotalValuePreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountUiItem(
                    account = Account.sampleMainnet().copy(
                        displayName = DisplayName("a very long name for my account")
                    ),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    ),
                    fiatTotalValue = FiatPrice(price = 3450.0.toDecimal192(), currency = SupportedCurrency.USD),
                    tag = AccountTag.DAPP_DEFINITION,
                    securityPrompts = listOf(SecurityPromptType.RECOVERY_REQUIRED),
                    isFiatBalanceVisible = true,
                    isLoadingAssets = false,
                    isLoadingBalance = false
                ),
                onApplySecuritySettingsClick = {}
            )
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountCardWithLongNameAndLongTotalValuePreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountUiItem(
                    account = Account.sampleMainnet().copy(
                        displayName = DisplayName("a very long name for my account again much more longer oh god ")
                    ),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    ),
                    fiatTotalValue = FiatPrice(price = 345008999008932.4.toDecimal192(), currency = SupportedCurrency.USD),
                    tag = AccountTag.DAPP_DEFINITION,
                    securityPrompts = listOf(
                        SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM,
                        SecurityPromptType.WRITE_DOWN_SEED_PHRASE,
                        SecurityPromptType.RECOVERY_REQUIRED
                    ),
                    isFiatBalanceVisible = true,
                    isLoadingAssets = false,
                    isLoadingBalance = false,
                ),
                onApplySecuritySettingsClick = {}
            )
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountCardWithLongNameAndTotalValueHiddenPreview() {
    RadixWalletPreviewTheme {
        CompositionLocalProvider(value = LocalBalanceVisibility.provides(false)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AccountCardView(
                    accountWithAssets = AccountUiItem(
                        account = Account.sampleMainnet().copy(
                            displayName = DisplayName("a very long name for my account again much more longer oh god ")
                        ),
                        assets = Assets(
                            tokens = emptyList(),
                            nonFungibles = listOf(),
                            poolUnits = emptyList(),
                            liquidStakeUnits = emptyList(),
                            stakeClaims = emptyList()
                        ),
                        fiatTotalValue = FiatPrice(price = 34509008998732.4.toDecimal192(), currency = SupportedCurrency.USD),
                        tag = AccountTag.DAPP_DEFINITION,
                        securityPrompts = listOf(SecurityPromptType.WALLET_NOT_RECOVERABLE),
                        isLoadingAssets = false,
                        isLoadingBalance = false,
                        isFiatBalanceVisible = true
                    ),
                    onApplySecuritySettingsClick = {}
                )
            }
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountCardLoadingPreview() {
    RadixWalletPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountUiItem(
                    account = Account.sampleMainnet(),
                    assets = Assets(
                        tokens = emptyList(),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        liquidStakeUnits = emptyList(),
                        stakeClaims = emptyList()
                    ),
                    fiatTotalValue = FiatPrice(price = 3450900899.0.toDecimal192(), currency = SupportedCurrency.USD),
                    tag = AccountTag.DAPP_DEFINITION,
                    securityPrompts = null,
                    isFiatBalanceVisible = true,
                    isLoadingAssets = true,
                    isLoadingBalance = true
                ),
                onApplySecuritySettingsClick = {}
            )
        }
    }
}
