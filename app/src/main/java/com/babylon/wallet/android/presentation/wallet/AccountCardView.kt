package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.AssetIconRowView
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
fun AccountCardView(
    address: String,
    accountName: String,
    isLegacyAccount: Boolean,
    showApplySecuritySettings: Boolean,
    needMnemonicRecovery: Boolean,
    assets: ImmutableList<AccountWithResources.FungibleResource>, // at the moment we pass only the tokens
    modifier: Modifier = Modifier,
    onApplySecuritySettings: () -> Unit,
    onMnemonicRecovery: () -> Unit,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = RadixTheme.dimensions.paddingDefault,
                    horizontal = RadixTheme.dimensions.paddingLarge
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accountName,
                    style = RadixTheme.typography.body1Header,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    color = RadixTheme.colors.white,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLegacyAccount) {
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.legacy_label),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.white
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            ActionableAddressView(
                address = address,
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(visible = showApplySecuritySettings, enter = fadeIn(), exit = fadeOut()) {
                ApplySecuritySettingsLabel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onApplySecuritySettings,
                    text = stringResource(id = com.babylon.wallet.android.R.string.apply_security_settings)
                )
            }
            AnimatedVisibility(visible = needMnemonicRecovery, enter = fadeIn(), exit = fadeOut()) {
                ApplySecuritySettingsLabel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMnemonicRecovery,
                    text = stringResource(id = com.babylon.wallet.android.R.string.recover_mnemonic)
                )
            }
            AnimatedVisibility(visible = false) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                AssetIconRowView(assets = assets)
            }
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun AccountCardPreview() {
    RadixWalletTheme {
        AccountCardView(
            address = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
            accountName = "My main account",
            isLegacyAccount = true,
            assets = persistentListOf(
                AccountWithResources.FungibleResource(
                    resourceAddress = "resource_address",
                    amount = BigDecimal.valueOf(237659),
                    nameMetadataItem = NameMetadataItem("cool XRD"),
                    symbolMetadataItem = SymbolMetadataItem("XRD")
                )
            ),
            modifier = Modifier.padding(bottom = 20.dp),
            onApplySecuritySettings = {},
            showApplySecuritySettings = true,
            needMnemonicRecovery = true,
            onMnemonicRecovery = {}
        )
    }
}
