package com.babylon.wallet.android.presentation.common.securityshields

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Suppress("ModifierMissing")
@Composable
fun ColumnScope.OrView() {
    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(R.string.transactionReview_updateShield_combinationLabel),
        style = RadixTheme.typography.body2Regular,
        color = RadixTheme.colors.textSecondary
    )

    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
}
