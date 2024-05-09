package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.XrdResource
import kotlin.random.Random

fun LazyListScope.feePayerSelectionContent(
    candidates: List<TransactionFeePayers.FeePayerCandidate>,
    selectedCandidateAddress: AccountAddress? = null,
    onPayerSelected: (Account) -> Unit
) {
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingXXXXLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            text = stringResource(id = R.string.customizeNetworkFees_selectFeePayer_navigationTitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
    items(candidates) { candidate ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            shape = RadixTheme.shapes.roundedRectMedium,
            colors = CardDefaults.cardColors(containerColor = RadixTheme.colors.defaultBackground),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = candidate.account.appearanceId.gradient(),
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
                    .padding(RadixTheme.dimensions.paddingDefault),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.account.displayName.value,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    style = RadixTheme.typography.body1Header,
                    color = Color.White
                )

                ActionableAddressView(
                    address = remember(candidate) {
                        candidate.account.address.asGeneral()
                    },
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.white.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClickable {
                        onPayerSelected(candidate.account)
                    }
                    .padding(start = RadixTheme.dimensions.paddingDefault)
                    .padding(vertical = RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RadixTheme.shapes.circle),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall))
                Text(
                    modifier = Modifier.weight(1f),
                    text = XrdResource.SYMBOL,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1
                )

                Text(
                    text = remember(candidate) {
                        candidate.xrdAmount.formatted()
                    },
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )

                RadioButton(
                    selected = candidate.account.address == selectedCandidateAddress,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = RadixTheme.colors.gray1,
                        unselectedColor = RadixTheme.colors.gray3,
                        disabledSelectedColor = Color.White
                    ),
                    onClick = {
                        onPayerSelected(candidate.account)
                    },
                )
            }
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun FeesPayersSelectionContentPreview() {
    val candidates = remember {
        Account.sampleMainnet.all.map {
            TransactionFeePayers.FeePayerCandidate(account = it, xrdAmount = Random.nextDouble(10000.0).toDecimal192())
        }
    }
    RadixWalletPreviewTheme {
        LazyColumn(modifier = Modifier.background(RadixTheme.colors.defaultBackground)) {
            feePayerSelectionContent(
                candidates = candidates,
                selectedCandidateAddress = null,
                onPayerSelected = {}
            )
        }
    }
}
