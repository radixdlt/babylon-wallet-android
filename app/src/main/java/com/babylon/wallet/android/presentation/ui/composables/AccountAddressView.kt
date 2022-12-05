package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun AccountAddressView(
    address: String,
    onCopyAccountAddressClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.defaultText
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Text(
            text = address,
            style = RadixTheme.typography.body2HighImportance,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        IconButton(
            modifier = Modifier.size(14.dp),
            onClick = {
                onCopyAccountAddressClick(address)
            },
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_copy),
                tint = contentColor,
                contentDescription = "copy account address"
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountAddressPreview() {
    AccountAddressView(
        address = "rdr1qsp98f4t5656563cq2qgtxg",
        onCopyAccountAddressClick = {},
        modifier = Modifier
    )
}
