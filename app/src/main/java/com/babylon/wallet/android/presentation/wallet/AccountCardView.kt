package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.presentation.ui.composables.AssetIconRowView
import com.babylon.wallet.android.presentation.ui.composables.TruncatedAddressText
import java.math.BigDecimal

@Suppress("UnstableCollections")
@Composable
fun AccountCardView(
    hashValue: String,
    accountName: String,
    accountValue: String,
    accountCurrency: String,
    onCopyClick: () -> Unit,
    assets: List<OwnedFungibleToken>, // at the moment we pass only the tokens
    modifier: Modifier = Modifier
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
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
                Text(
                    text = "$accountCurrency$accountValue",
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.white
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TruncatedAddressText(
                    modifier = Modifier.weight(1f, false),
                    text = hashValue,
                    color = RadixTheme.colors.white,
                    style = RadixTheme.typography.body2HighImportance,
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        onCopyClick()
                    },
                ) {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_copy),
                        null,
                        tint = RadixTheme.colors.white
                    )
                }
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            AssetIconRowView(assets = assets)
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun AccountCardPreview() {
    BabylonWalletTheme {
        AccountCardView(
            hashValue = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
            accountName = "My main account",
            accountValue = "19195",
            accountCurrency = "$",
            onCopyClick = {},
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
