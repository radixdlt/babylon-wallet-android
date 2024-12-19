package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun AddFactorButton(
    onClick: () -> Unit
) {
    RadixSecondaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(41.dp),
        text = stringResource(id = R.string.plus),
        textStyle = RadixTheme.typography.header,
        onClick = onClick
    )
}
