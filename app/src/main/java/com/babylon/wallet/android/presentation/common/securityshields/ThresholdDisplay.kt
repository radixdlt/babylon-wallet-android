package com.babylon.wallet.android.presentation.common.securityshields

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.radixdlt.sargon.Threshold
import java.util.Locale

@Composable
fun Threshold.display(): String = when (this) {
    is Threshold.All -> stringResource(R.string.common_all).uppercase(Locale.getDefault())
    is Threshold.Specific -> "${v1.toInt()}"
}
