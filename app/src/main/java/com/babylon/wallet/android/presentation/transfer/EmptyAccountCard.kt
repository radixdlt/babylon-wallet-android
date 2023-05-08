package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun EmptyAccountCard(
    modifier: Modifier = Modifier,
    onChooseAccountClick: () -> Unit,
    onAddAssetsClick: () -> Unit,
    onCancelClick: () -> Unit,
    isCancelable: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        Row(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingSmall),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixTextButton(
                text = stringResource(id = R.string.choose_accounts),
                contentColor = RadixTheme.colors.gray2,
                onClick = onChooseAccountClick
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isCancelable) {
                IconButton(onClick = onCancelClick) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear"
                    )
                }
            }
        }

        Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixTextButton(
                text = stringResource(id = R.string.add_assets),
                contentColor = RadixTheme.colors.gray2,
                onClick = onAddAssetsClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyAccountCardPreview() {
    RadixWalletTheme {
        EmptyAccountCard(
            onChooseAccountClick = {},
            onAddAssetsClick = {},
            onCancelClick = {}
        )
    }
}
