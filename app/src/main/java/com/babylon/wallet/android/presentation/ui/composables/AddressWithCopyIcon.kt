package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.truncatedHash

@Composable
fun AddressWithCopyIcon(
    address: String,
    modifier: Modifier = Modifier,
    contentColor: Color = RadixTheme.colors.white.copy(alpha = 0.8f)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
    ) {
        Text(
            modifier = Modifier.weight(1f, false),
            text = address.truncatedHash(),
            color = contentColor,
            style = RadixTheme.typography.body2HighImportance,
            maxLines = 1
        )
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = R.drawable.ic_copy),
            contentDescription = null,
            tint = contentColor,
        )
    }
}
