package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun SimpleAccountCard(
    modifier: Modifier = Modifier,
    account: AccountItemUiModel,
    vertical: Boolean = false
) {
    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                text = account.displayName.orEmpty(),
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = White,
                overflow = TextOverflow.Ellipsis
            )
            ActionableAddressView(
                address = Address.Account(account.address),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = White.copy(alpha = 0.8f)
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                text = account.displayName.orEmpty(),
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                color = White,
                overflow = TextOverflow.Ellipsis
            )
            ActionableAddressView(
                address = Address.Account(account.address),
                textStyle = RadixTheme.typography.body2HighImportance,
                textColor = White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SimpleAccountCard(
    modifier: Modifier = Modifier,
    account: Account,
    shape: Shape = RadixTheme.shapes.roundedRectSmall
) {
    Row(
        modifier = modifier
            .background(
                brush = account.appearanceId.gradient(),
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = account.displayName.value,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = White,
            overflow = TextOverflow.Ellipsis
        )
        ActionableAddressView(
            address = remember(account.address) {
                Address.Account(account.address)
            },
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun SimpleAccountCardWithAddress(
    modifier: Modifier = Modifier,
    account: Account
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(top = RadixTheme.dimensions.paddingLarge),
            text = account.displayName.value,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            color = White,
            overflow = TextOverflow.Ellipsis
        )
        ActionableAddressView(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingLarge),
            address = remember(account.address) {
                Address.Account(account.address)
            },
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = White.copy(alpha = 0.8f)
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun SimpleAccountCardPreview() {
    RadixWalletTheme {
        SimpleAccountCard(account = Account.sampleMainnet(), modifier = Modifier.fillMaxWidth())
    }
}

@UsesSampleValues
@Preview
@Composable
fun SimpleAccountCardWithoutNamePreview() {
    RadixWalletTheme {
        SimpleAccountCard(account = Account.sampleMainnet().copy(displayName = DisplayName("")), modifier = Modifier.fillMaxWidth())
    }
}
