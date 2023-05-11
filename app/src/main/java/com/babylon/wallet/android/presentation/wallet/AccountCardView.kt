package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
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
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import rdx.works.profile.data.utils.isOlympiaAccount
import java.math.BigDecimal

@Composable
fun AccountCardView(
    accountWithResources: AccountWithResources,
    isPromptVisible: Boolean,
    modifier: Modifier = Modifier,
    onApplySecuritySettings: () -> Unit,
    onMnemonicRecovery: () -> Unit,
) {
    val gradient = remember(accountWithResources.account.appearanceID) {
        AccountGradientList[accountWithResources.account.appearanceID % AccountGradientList.size]
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
            text = accountWithResources.account.displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 2,
            color = RadixTheme.colors.white,
            overflow = TextOverflow.Ellipsis
        )

        ActionableAddressView(
            modifier = Modifier.constrainAs(addressLabel) {
                top.linkTo(nameLabel.bottom, margin = 8.dp)
                start.linkTo(parent.start)
            },
            address = accountWithResources.account.address,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
        )

        if (accountWithResources.account.isOlympiaAccount()) {
            Text(
                modifier = Modifier.constrainAs(legacyLabel) {
                    start.linkTo(addressLabel.end, margin = 8.dp)
                    bottom.linkTo(addressLabel.bottom)
                },
                text = stringResource(id = R.string.legacy_label),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.white
            )
        }

        Spacer(modifier = Modifier.constrainAs(spacer) {
            linkTo(
                start = parent.start,
                end = parent.end,
                top = addressLabel.bottom,
                bottom = assetsContainer.top,
            )
            height = Dimension.value(32.dp)
        })

        AccountAssetsRow(
            modifier = Modifier.constrainAs(assetsContainer) {
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    top = spacer.bottom,
                    bottom = if (isPromptVisible) promptsContainer.top else parent.bottom,
                    bottomMargin = if (isPromptVisible) 18.dp else 0.dp
                )
                width = Dimension.fillToConstraints
            },
            resources = accountWithResources.resources
        )

        AnimatedVisibility(
            modifier = Modifier.constrainAs(promptsContainer) {
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    top = assetsContainer.bottom,
                    bottom = parent.bottom,
                    verticalBias = 1f
                )
            },
            visible = isPromptVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                ApplySecuritySettingsLabel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (accountWithResources.needMnemonicBackup()) {
                            onApplySecuritySettings()
                        } else {
                            onMnemonicRecovery()
                        }
                    },
                    text = stringResource(id = R.string.apply_security_settings)
                )
            }
        }



    }
}
@Preview
@Composable
fun AccountCardPreview() {
    RadixWalletTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountCardView(
                accountWithResources = AccountWithResources(
                    account = SampleDataProvider().sampleAccount(),
                    resources = Resources(
                        fungibleResources = listOf(
                            AccountWithResources.FungibleResource(
                                resourceAddress = "resource_address",
                                amount = BigDecimal.valueOf(237659),
                                nameMetadataItem = NameMetadataItem("cool XRD"),
                                symbolMetadataItem = SymbolMetadataItem("XRD")
                            )
                        ),
                        nonFungibleResources = listOf()
                    )
                ),
                isPromptVisible = true,
                onApplySecuritySettings = {},
                onMnemonicRecovery = {}
            )
        }
    }
}
