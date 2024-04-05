package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.extensions.init

@Composable
fun AccountCardWithStack(
    modifier: Modifier,
    appearanceId: Int = 0,
    accountName: String,
    accountAddress: String,
) {
    val numberOfOtherCards = 4
    val singleCardHeight = 80.dp
    val offset = 6.dp
    Box(modifier = modifier.height(singleCardHeight + offset * numberOfOtherCards)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(singleCardHeight)
                .zIndex(5f)
                .background(
                    Brush.horizontalGradient(AccountGradientList[appearanceId % AccountGradientList.size]),
                    RadixTheme.shapes.roundedRectSmall
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
                    color = Color.White
                )

                val address = remember(accountAddress) {
                    runCatching { AccountAddress.init(accountAddress) }.getOrNull()
                }
                if (address != null) {
                    ActionableAddressView(
                        address = Address.Account(address),
                        textStyle = RadixTheme.typography.body2Regular,
                        textColor = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        repeat(4) {
            val index = it + 1
            val nextColors =
                AccountGradientList[(appearanceId + index) % AccountGradientList.size]
                    .map { color -> color.copy(alpha = 0.3f) }
            Box(
                modifier = Modifier
                    .offset(y = offset * index)
                    .fillMaxWidth(1f - 0.05f * (index))
                    .height(singleCardHeight)
                    .zIndex(5f - index)
                    .background(
                        Brush.horizontalGradient(nextColors),
                        RadixTheme.shapes.roundedRectSmall
                    )
                    .align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardStackPreview() {
    AccountCardWithStack(
        modifier = Modifier,
        accountName = "My account",
        accountAddress = "d32d32"
    )
}
