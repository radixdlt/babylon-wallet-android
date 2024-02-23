package com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail

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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.ret.RetBridge

@Composable
fun UnknownComponentsSheetContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    unknownComponentAddresses: ImmutableList<String>
) {
    val isPools = remember(unknownComponentAddresses) {
        unknownComponentAddresses.all { RetBridge.Address.isPool(it) }
    }

    Column(modifier = modifier) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            title = stringResource(
                id = if (isPools) R.string.transactionReview_unknownPools else R.string.transactionReview_unknownComponents,
                unknownComponentAddresses.size
            ),
            onDismissRequest = onBackClick
        )
        HorizontalDivider(color = RadixTheme.colors.gray5)
        LazyColumn {
            itemsIndexed(unknownComponentAddresses) { index, unknownComponentAddress ->
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
                            val isPool = remember(unknownComponentAddress) {
                                RetBridge.Address.isPool(unknownComponentAddress)
                            }
                            Text(
                                text = if (isPool) {
                                    stringResource(id = R.string.common_pool)
                                } else {
                                    stringResource(id = R.string.common_component)
                                },
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.gray1,
                                overflow = TextOverflow.Ellipsis
                            )

                            ActionableAddressView(
                                address = unknownComponentAddress,
                                textStyle = RadixTheme.typography.body2Regular,
                                textColor = RadixTheme.colors.gray1
                            )
                        }
                    }

                    if (index != unknownComponentAddresses.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = RadixTheme.colors.gray5
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnknownDAppComponentsSheetContentPreview() {
    RadixWalletTheme {
        UnknownComponentsSheetContent(
            onBackClick = {},
            unknownComponentAddresses = persistentListOf(
                "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp",
                "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z16qp"
            )
        )
    }
}
