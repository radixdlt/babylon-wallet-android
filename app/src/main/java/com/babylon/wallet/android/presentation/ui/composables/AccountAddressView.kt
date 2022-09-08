package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R

@Composable
fun AccountAddressView(
    address: String,
    onCopyAccountAddressClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        ResponsiveText(
            text = address,
            style = MaterialTheme.typography.h6,
            maxLines = 1
        )
        IconButton(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(14.dp),
            onClick = {
                onCopyAccountAddressClick(address)
            },
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
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
