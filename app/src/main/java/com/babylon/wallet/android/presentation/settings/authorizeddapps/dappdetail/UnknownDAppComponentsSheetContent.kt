package com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UnknownDAppComponentsSheetContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    unknownDAppComponents: ImmutableList<String>
) {
    Column(modifier = modifier) {
        val title = stringResource(id = R.string.transactionReview_unknownComponents, unknownDAppComponents.size)
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            title = title,
            onDismissRequest = onBackClick
        )
        HorizontalDivider(color = RadixTheme.colors.gray5)
        val lastItem = unknownDAppComponents.last()
        LazyColumn(
            contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            items(unknownDAppComponents) { unknownComponentAddress ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingSmall)
                            .padding(vertical = RadixTheme.dimensions.paddingDefault),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Thumbnail.DApp(
                            modifier = Modifier.size(44.dp),
                            dapp = null,
                            shape = RadixTheme.shapes.roundedRectXSmall
                        )
                        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))
                        Column {
                            Text(
                                text = stringResource(id = R.string.common_component),
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

                    if (lastItem != unknownComponentAddress) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(),
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
        UnknownDAppComponentsSheetContent(
            onBackClick = {},
            unknownDAppComponents = persistentListOf(
                "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp",
                "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z16qp"
            )
        )
    }
}
