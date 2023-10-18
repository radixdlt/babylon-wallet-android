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
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountWithAssets
import com.babylon.wallet.android.domain.model.Assets
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import com.babylon.wallet.android.domain.model.XrdResource
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.toText
import java.math.BigDecimal

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
fun AccountCardView(
    accountWithAssets: AccountWithAssets,
    accountTag: WalletUiState.AccountTag?,
    isLoadingResources: Boolean,
    securityPromptType: SecurityPromptType?,
    modifier: Modifier = Modifier,
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
        val (nameLabel, legacyLabel, addressLabel, spacer, assetsContainer, promptsContainer) = createRefs()

        Text(
            modifier = Modifier.constrainAs(nameLabel) {
                linkTo(start = parent.start, end = parent.end, bias = 0f)
                top.linkTo(parent.top)
            },
            text = accountWithAssets.account.displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
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
    RadixWalletTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithAssets = AccountWithAssets(
                    account = SampleDataProvider().sampleAccount(),
                    assets = Assets(
                        fungibles = listOf(
                            Resource.FungibleResource(
                                resourceAddress = XrdResource.address(),
                                ownedAmount = BigDecimal.valueOf(237659),
                                nameMetadataItem = NameMetadataItem("cool XRD"),
                                symbolMetadataItem = SymbolMetadataItem("XRD")
                            )
                        ),
                        nonFungibles = listOf(),
                        poolUnits = emptyList(),
                        validatorsWithStakeResources = ValidatorsWithStakeResources()
                    )
                ),
                accountTag = WalletUiState.AccountTag.DAPP_DEFINITION,
                isLoadingResources = false,
                securityPromptType = SecurityPromptType.NEEDS_RESTORE,
                onApplySecuritySettings = {}
            )
        }
    }
}
