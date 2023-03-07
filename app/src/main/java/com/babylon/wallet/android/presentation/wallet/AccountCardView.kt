package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddress
import com.babylon.wallet.android.presentation.ui.composables.AssetIconRowView
import java.math.BigDecimal

@Suppress("UnstableCollections")
@Composable
fun AccountCardView(
    address: String,
    accountName: String,
    assets: List<OwnedFungibleToken>, // at the moment we pass only the tokens
    modifier: Modifier = Modifier,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accountName,
                    style = RadixTheme.typography.body1Header,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    color = RadixTheme.colors.white
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            ActionableAddress(
                address = address,
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
            )
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
            assets = listOf(
                OwnedFungibleToken(
                    AccountAddress("123"),
                    BigDecimal.valueOf(100000),
                    "1234",
                    FungibleToken(
                        "1234",
                        totalSupply = BigDecimal.valueOf(10000000000),
                        totalMinted = BigDecimal.valueOf(1000000),
                        totalBurnt = BigDecimal.valueOf(100),
                        metadata = mapOf("symbol" to "XRD")
                    )
                )
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}
