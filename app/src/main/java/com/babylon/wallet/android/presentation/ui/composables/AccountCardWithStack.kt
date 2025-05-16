package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.extensions.init

@Composable
fun AccountCardWithStack(
    modifier: Modifier,
    appearanceId: AppearanceId,
    accountName: String,
    accountAddress: String,
) {
    val numberOfOtherCards = 3
    val singleCardHeight = 91.dp
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(singleCardHeight)
                .zIndex(4f)
                .background(
                    appearanceId.gradient(),
                    RadixTheme.shapes.roundedRectMedium
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Text(
                    text = accountName,
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Header,
                    color = White
                )

                val address = remember(accountAddress) {
                    runCatching { AccountAddress.init(accountAddress) }.getOrNull()
                }
                if (address != null) {
                    ActionableAddressView(
                        address = Address.Account(address),
                        textStyle = RadixTheme.typography.body2HighImportance,
                        textColor = White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        repeat(numberOfOtherCards) {
            val index = it + 1
            val nextAppearanceIdIndex = appearanceId.value + index.toUInt()
            val topOffset = (if (index == numberOfOtherCards) 11.dp else 12.dp) * index
            val bgAlpha = if (index == numberOfOtherCards) 0.1f else 0.2f

            Box(
                modifier = Modifier
                    .padding(top = topOffset)
                    .fillMaxWidth(1f - 0.05f * (index))
                    .height(singleCardHeight)
                    .zIndex(4f - index)
                    .background(
                        AppearanceId(nextAppearanceIdIndex.toUByte())
                            .gradient(
                                alpha = bgAlpha
                            ),
                        RadixTheme.shapes.roundedRectMedium
                    )
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardStackPreview() {
    RadixWalletTheme {
        AccountCardWithStack(
            modifier = Modifier,
            accountName = "My account",
            accountAddress = "d32d32",
            appearanceId = AppearanceId(0u)
        )
    }
}
