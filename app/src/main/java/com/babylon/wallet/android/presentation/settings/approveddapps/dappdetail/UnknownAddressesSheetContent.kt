package com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UnknownAddressesSheetContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    unknownAddresses: ImmutableList<Address>
) {
    val isPools = remember(unknownAddresses) {
        unknownAddresses.all { it is Address.Pool }
    }

    Column(modifier = modifier) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            title = stringResource(
                id = if (isPools) R.string.interactionReview_unknownPools else R.string.interactionReview_unknownComponents,
                unknownAddresses.size
            ),
            onDismissRequest = onBackClick
        )
        HorizontalDivider(color = RadixTheme.colors.divider)
        LazyColumn {
            itemsIndexed(unknownAddresses) { index, unknownComponentAddress ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        Thumbnail.DApp(
                            modifier = Modifier.size(44.dp),
                            dapp = null,
                            shape = RadixTheme.shapes.roundedRectXSmall
                        )

                        Column {
                            Text(
                                text = when (unknownComponentAddress) {
                                    is Address.Pool -> stringResource(id = R.string.common_pool)
                                    is Address.Component -> stringResource(id = R.string.common_component)
                                    else -> stringResource(id = R.string.empty)
                                },
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis
                            )

                            ActionableAddressView(
                                address = unknownComponentAddress,
                                textStyle = RadixTheme.typography.body2Regular,
                                textColor = RadixTheme.colors.text
                            )
                        }
                    }

                    if (index != unknownAddresses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = RadixTheme.colors.divider
                        )
                    }
                }
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun UnknownDAppComponentsSheetContentPreview() {
    RadixWalletTheme {
        UnknownAddressesSheetContent(
            onBackClick = {},
            unknownAddresses = persistentListOf(
                Address.Component(ComponentAddress.sampleMainnet()),
                Address.Component(ComponentAddress.sampleMainnet.other())
            )
        )
    }
}
